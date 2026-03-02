package com.finance.service;

import com.finance.model.NewsArticle;
import com.finance.model.NewsCategory;
import com.finance.repository.NewsArticleRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Dış kaynaklardan (RSS feed) finans haberlerini çeken ve
 * veritabanına kaydeden servis.
 *
 * <p>
 * Haber kaynakları {@code application.yml} içindeki
 * {@code finance.data-sources.news-rss-feeds} listesinden okunur.
 * </p>
 */
@Service
public class NewsFetchService {

    private static final Logger logger = LogManager.getLogger(NewsFetchService.class);

    private final NewsArticleRepository newsRepository;
    private final HttpClient httpClient;

    /** RSS feed URL listesi – application.yml'den okunur */
    @Value("${finance.data-sources.news-rss-feeds[0]:https://www.bloomberght.com/rss}")
    private String rssFeed1;

    @Value("${finance.data-sources.news-rss-feeds[1]:https://www.ntv.com.tr/ekonomi.rss}")
    private String rssFeed2;

    @Value("${finance.data-sources.news-rss-feeds[2]:https://bigpara.hurriyet.com.tr/rss/}")
    private String rssFeed3;

    public NewsFetchService(NewsArticleRepository newsRepository) {
        this.newsRepository = newsRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /*
     * ================================================================
     * SCHEDULED TASK – Otomatik Haber Güncelleme
     * ================================================================
     */

    /**
     * Yapılandırılabilir cron ile haberleri periyodik çeker.
     * Varsayılan: her 30 dakikada bir.
     */
    @Scheduled(cron = "${finance.scheduler.news-fetch-cron}")
    @CacheEvict(value = "news", allEntries = true)
    public void scheduledNewsFetch() {
        logger.info("⏰ Zamanlanmış haber güncelleme başlatılıyor...");
        try {
            List<String> feeds = List.of(rssFeed1, rssFeed2, rssFeed3);
            int totalSaved = 0;
            for (String feedUrl : feeds) {
                try {
                    List<NewsArticle> articles = fetchAndSaveFromRss(feedUrl);
                    totalSaved += articles.size();
                } catch (Exception e) {
                    logger.warn("RSS feed atlandı (hata): {} – {}", feedUrl, e.getMessage());
                }
            }
            logger.info("✅ Haber güncelleme tamamlandı – {} yeni haber kaydedildi.", totalSaved);
        } catch (Exception e) {
            logger.error("❌ Haber güncelleme başarısız!", e);
        }
    }

    /*
     * ================================================================
     * RSS PARSE & KAYIT
     * ================================================================
     */

    /**
     * Verilen RSS feed URL'sinden haberleri çeker, parse eder ve kaydeder.
     *
     * @param feedUrl RSS feed URL
     * @return Kaydedilen haber listesi
     */
    @Transactional
    public List<NewsArticle> fetchAndSaveFromRss(String feedUrl) {
        logger.info("RSS feed'den haberler çekiliyor: {}", feedUrl);
        List<NewsArticle> savedArticles = new ArrayList<>();

        try {
            // ── 1. RSS XML İndir ─────────────────────────────────────
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(feedUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "FinancePortal/1.0")
                    .header("Accept", "application/rss+xml, application/xml, text/xml")
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                logger.error("RSS servisi HTTP {} döndürdü: {}", response.statusCode(), feedUrl);
                return savedArticles;
            }

            // ── 2. XML Parse ─────────────────────────────────────────
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(response.body());
            doc.getDocumentElement().normalize();

            // Kaynağı belirle (<channel><title>)
            String sourceName = extractSourceName(doc, feedUrl);

            // ── 3. Her <item> Parse Et ───────────────────────────────
            NodeList items = doc.getElementsByTagName("item");
            logger.debug("RSS feed'de {} item bulundu: {}", items.getLength(), feedUrl);

            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);

                String title = getElementText(item, "title");
                String link = getElementText(item, "link");
                String description = getElementText(item, "description");
                String pubDate = getElementText(item, "pubDate");

                if (title.isBlank())
                    continue;

                // Benzersiz ID oluştur (link veya title hash)
                String externalId = link.isBlank()
                        ? UUID.nameUUIDFromBytes(title.getBytes()).toString()
                        : UUID.nameUUIDFromBytes(link.getBytes()).toString();

                // Duplicate kontrolü
                if (newsRepository.existsByExternalId(externalId)) {
                    continue;
                }

                NewsArticle article = new NewsArticle();
                article.setTitle(cleanHtml(title));
                article.setSummary(cleanHtml(description));
                article.setUrl(link);
                article.setSource(sourceName);
                article.setExternalId(externalId);
                article.setCategory(categorizeArticle(title, description));
                article.setPublishedAt(parseRssDate(pubDate));

                // Görsel URL (varsa)
                String imageUrl = extractImageUrl(item);
                if (imageUrl != null) {
                    article.setImageUrl(imageUrl);
                }

                savedArticles.add(article);
            }

            // ── 4. Toplu Kaydet ──────────────────────────────────────
            if (!savedArticles.isEmpty()) {
                newsRepository.saveAll(savedArticles);
                logger.info("{} yeni haber kaydedildi – kaynak: {}", savedArticles.size(), sourceName);
            }

        } catch (Exception e) {
            logger.error("RSS parse hatası ({}): {}", feedUrl, e.getMessage(), e);
            throw new RuntimeException("RSS haber çekme başarısız: " + feedUrl, e);
        }

        return savedArticles;
    }

    /*
     * ================================================================
     * CACHE'Lİ SORGULAR
     * ================================================================
     */

    /** Sayfalı haber listesi (cache'li) */
    @Cacheable(value = "news", key = "'page-' + #page + '-' + #size")
    public Page<NewsArticle> getLatestNews(int page, int size) {
        return newsRepository.findAllByOrderByPublishedAtDesc(
                PageRequest.of(page, size));
    }

    /** Kategoriye göre haber listesi */
    @Cacheable(value = "news", key = "'cat-' + #category + '-' + #page")
    public Page<NewsArticle> getNewsByCategory(NewsCategory category, int page, int size) {
        return newsRepository.findByCategoryOrderByPublishedAtDesc(
                category, PageRequest.of(page, size));
    }

    /** Başlıkta arama */
    public Page<NewsArticle> searchNews(String keyword, int page, int size) {
        return newsRepository.findByTitleContainingIgnoreCaseOrderByPublishedAtDesc(
                keyword, PageRequest.of(page, size));
    }

    /** Tek haber detayı */
    public Optional<NewsArticle> getNewsById(Long id) {
        return newsRepository.findById(id);
    }

    /** Mevcut kategorileri döner */
    public List<NewsCategory> getCategories() {
        return Arrays.asList(NewsCategory.values());
    }

    /*
     * ================================================================
     * YARDIMCI METOTLAR
     * ================================================================
     */

    /**
     * Basit anahtar kelime bazlı haber kategorilendirme.
     * Doküman isteri: "Haberler kategorilere ayrılabilmeli"
     */
    private NewsCategory categorizeArticle(String title, String description) {
        String text = (title + " " + description).toLowerCase(Locale.forLanguageTag("tr"));

        if (containsAny(text, "dolar", "euro", "döviz", "kur", "usd", "eur", "gbp", "fx"))
            return NewsCategory.DOVIZ;
        if (containsAny(text, "hisse", "borsa", "bist", "endeks", "halk bank", "garanti"))
            return NewsCategory.HISSE;
        if (containsAny(text, "tahvil", "bono", "faiz", "hazine"))
            return NewsCategory.TAHVIL_BONO;
        if (containsAny(text, "fon", "yatırım fonu", "portföy"))
            return NewsCategory.FON;
        if (containsAny(text, "kripto", "bitcoin", "ethereum", "btc", "eth"))
            return NewsCategory.KRIPTO;
        if (containsAny(text, "altın", "petrol", "gümüş", "emtia", "commodity"))
            return NewsCategory.EMTIA;
        if (containsAny(text, "fed", "ecb", "imf", "dünya", "global", "abd"))
            return NewsCategory.DUNYA;
        if (containsAny(text, "enflasyon", "büyüme", "gsyih", "merkez bankası", "tcmb", "ekonomi"))
            return NewsCategory.GENEL_EKONOMI;

        return NewsCategory.GENEL_EKONOMI;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw))
                return true;
        }
        return false;
    }

    /** RSS <pubDate> formatını parse et (RFC 822). */
    private LocalDateTime parseRssDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank())
            return LocalDateTime.now();
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(dateStr,
                    DateTimeFormatter.RFC_1123_DATE_TIME);
            return zdt.toLocalDateTime();
        } catch (Exception e) {
            logger.debug("RSS tarih parse edilemedi: '{}' – şimdi kullanılıyor.", dateStr);
            return LocalDateTime.now();
        }
    }

    /** RSS kanalının adını çıkar. */
    private String extractSourceName(Document doc, String feedUrl) {
        try {
            NodeList channels = doc.getElementsByTagName("channel");
            if (channels.getLength() > 0) {
                Element channel = (Element) channels.item(0);
                String title = getElementText(channel, "title");
                if (!title.isBlank())
                    return title;
            }
        } catch (Exception ignored) {
        }
        // Fallback: URL'den domain çıkar
        try {
            return URI.create(feedUrl).getHost();
        } catch (Exception e) {
            return "Bilinmeyen Kaynak";
        }
    }

    /** RSS item'ından görsel URL çıkar (enclosure veya media:content). */
    private String extractImageUrl(Element item) {
        // <enclosure url="..." type="image/..."/>
        NodeList enclosures = item.getElementsByTagName("enclosure");
        for (int i = 0; i < enclosures.getLength(); i++) {
            Element enc = (Element) enclosures.item(i);
            String type = enc.getAttribute("type");
            if (type != null && type.startsWith("image")) {
                return enc.getAttribute("url");
            }
        }
        // <media:content url="..."/>
        NodeList mediaContent = item.getElementsByTagName("media:content");
        if (mediaContent.getLength() > 0) {
            return ((Element) mediaContent.item(0)).getAttribute("url");
        }
        return null;
    }

    /** XML element text değerini döner. */
    private String getElementText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0)
            return "";
        Node node = nodes.item(0);
        return node.getTextContent() != null ? node.getTextContent().trim() : "";
    }

    /** Basit HTML temizleme (CDATA / tag kaldırma). */
    private String cleanHtml(String input) {
        if (input == null)
            return "";
        return input
                .replaceAll("<[^>]+>", "") // HTML tag'leri kaldır
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"")
                .replaceAll("&#39;", "'")
                .replaceAll("\\s+", " ") // Çoklu boşluk → tek boşluk
                .trim();
    }
}
