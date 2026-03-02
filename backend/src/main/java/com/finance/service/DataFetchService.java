package com.finance.service;

import com.finance.model.Currency;
import com.finance.repository.CurrencyRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * TCMB XML servisinden güncel döviz kurlarını çeken ve
 * PostgreSQL veritabanına kaydeden servis.
 *
 * <p>
 * Veri kaynağı URL'si {@code application.yml} dosyasından
 * ({@code finance.data-sources.tcmb-url}) okunur.
 * </p>
 *
 * <p>
 * {@code @Scheduled} ile yapılandırılabilir cron ifadesine göre
 * otomatik olarak çalışır.
 * </p>
 */
@Service
public class DataFetchService {

    private static final Logger logger = LogManager.getLogger(DataFetchService.class);
    private static final String SOURCE_TCMB = "TCMB";

    private final CurrencyRepository currencyRepository;
    private final HttpClient httpClient;

    /** TCMB XML endpoint – application.yml'den okunur */
    @Value("${finance.data-sources.tcmb-url}")
    private String tcmbUrl;

    public DataFetchService(CurrencyRepository currencyRepository) {
        this.currencyRepository = currencyRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /*
     * ================================================================
     * SCHEDULED TASK – Otomatik Kur Güncelleme
     * ================================================================
     */

    /**
     * Yapılandırılabilir cron ifadesine göre TCMB kurlarını çeker.
     * Varsayılan: her 15 dakikada bir
     * ({@code finance.scheduler.currency-fetch-cron}).
     */
    @Scheduled(cron = "${finance.scheduler.currency-fetch-cron}")
    @CacheEvict(value = "currencies", allEntries = true)
    public void scheduledCurrencyFetch() {
        logger.info("⏰ Zamanlanmış TCMB kur güncelleme başlatılıyor...");
        try {
            List<Currency> fetched = fetchAndSaveTcmbRates();
            logger.info("✅ TCMB kur güncelleme tamamlandı – {} kur işlendi.", fetched.size());
        } catch (Exception e) {
            logger.error("❌ TCMB kur güncelleme başarısız!", e);
        }
    }

    /*
     * ================================================================
     * TCMB XML PARSE & KAYIT
     * ================================================================
     */

    /**
     * TCMB günlük kur XML'ini indirir, parse eder ve veritabanına kaydeder.
     *
     * @return Kaydedilen Currency listesi
     */
    @Transactional
    public List<Currency> fetchAndSaveTcmbRates() {
        logger.info("TCMB kurları çekiliyor: {}", tcmbUrl);

        List<Currency> currencies = new ArrayList<>();

        try {
            // ── 1. XML İndir ─────────────────────────────────────────
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tcmbUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/xml")
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                logger.error("TCMB XML servisi HTTP {} döndürdü", response.statusCode());
                return currencies;
            }

            // ── 2. XML Parse ─────────────────────────────────────────
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // XXE koruması
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(response.body());
            doc.getDocumentElement().normalize();

            // Tarih bilgisini al (Tarih_Date attribute: "03/02/2026")
            Element root = doc.getDocumentElement();
            String dateStr = root.getAttribute("Tarih");
            LocalDate rateDate = parseDate(dateStr);

            logger.debug("TCMB kur tarihi: {}", rateDate);

            // ── 3. Her Currency node'unu parse et ────────────────────
            NodeList currencyNodes = doc.getElementsByTagName("Currency");

            for (int i = 0; i < currencyNodes.getLength(); i++) {
                Element element = (Element) currencyNodes.item(i);

                String code = element.getAttribute("CurrencyCode");
                if (code == null || code.isBlank())
                    continue;

                // Aynı tarih ve kaynak için tekrar kayıt oluşturma
                if (currencyRepository.existsByCurrencyCodeAndDateAndSource(
                        code, rateDate, SOURCE_TCMB)) {
                    logger.debug("Kur zaten mevcut: {} – {}", code, rateDate);
                    continue;
                }

                Currency currency = new Currency();
                currency.setCurrencyCode(code);
                currency.setCurrencyName(getElementText(element, "Isim"));
                currency.setUnit(parseIntSafe(getElementText(element, "Unit"), 1));
                currency.setForexBuying(parseBigDecimal(getElementText(element, "ForexBuying")));
                currency.setForexSelling(parseBigDecimal(getElementText(element, "ForexSelling")));
                currency.setBanknoteBuying(parseBigDecimal(getElementText(element, "BanknoteBuying")));
                currency.setBanknoteSelling(parseBigDecimal(getElementText(element, "BanknoteSelling")));
                currency.setDate(rateDate);
                currency.setSource(SOURCE_TCMB);

                currencies.add(currency);
                logger.debug("Kur parse edildi: {} – Alış: {}, Satış: {}",
                        code, currency.getForexBuying(), currency.getForexSelling());
            }

            // ── 4. Toplu Kaydet ──────────────────────────────────────
            if (!currencies.isEmpty()) {
                currencyRepository.saveAll(currencies);
                logger.info("{} adet döviz kuru veritabanına kaydedildi.", currencies.size());
            }

        } catch (Exception e) {
            logger.error("TCMB XML parse hatası: {}", e.getMessage(), e);
            throw new RuntimeException("TCMB veri çekme başarısız", e);
        }

        return currencies;
    }

    /*
     * ================================================================
     * CACHE'Lİ SORGULAR
     * ================================================================
     */

    /** Bugünkü tüm kurları döner (cache'li). */
    @Cacheable(value = "currencies", key = "'today'")
    public List<Currency> getTodayRates() {
        return currencyRepository.findByDate(LocalDate.now());
    }

    /**
     * Belirli bir döviz kodu için tarih aralığında kurları döner (tarihsel veri).
     */
    @Cacheable(value = "currencies", key = "#currencyCode + '-' + #from + '-' + #to")
    public List<Currency> getHistoricalRates(String currencyCode, LocalDate from, LocalDate to) {
        return currencyRepository.findByCurrencyCodeAndDateBetweenOrderByDateAsc(
                currencyCode, from, to);
    }

    /*
     * ================================================================
     * YARDIMCI METOTLAR
     * ================================================================
     */

    /** TCMB tarih formatını parse eder ("dd.MM.yyyy" veya "MM/dd/yyyy"). */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            logger.warn("Tarih bilgisi boş – bugünün tarihi kullanılıyor.");
            return LocalDate.now();
        }
        try {
            // TCMB "dd.MM.yyyy" formatı
            if (dateStr.contains(".")) {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            }
            // Alternatif "MM/dd/yyyy"
            return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        } catch (Exception e) {
            logger.warn("Tarih parse edilemedi: '{}' – bugünün tarihi kullanılıyor.", dateStr);
            return LocalDate.now();
        }
    }

    /** XML element içindeki text değerini döner. */
    private String getElementText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0)
            return "";
        Node node = nodes.item(0);
        return node.getTextContent() != null ? node.getTextContent().trim() : "";
    }

    /** String → BigDecimal dönüşümü (boş/hatalı değerlerde null döner). */
    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank())
            return null;
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            logger.warn("BigDecimal parse hatası: '{}'", value);
            return null;
        }
    }

    /** String → int dönüşümü (hata durumunda default değer döner). */
    private int parseIntSafe(String value, int defaultValue) {
        if (value == null || value.isBlank())
            return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
