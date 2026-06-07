package com.finance.controller;

import com.finance.dto.ApiResponse;
import com.finance.model.Currency;
import com.finance.repository.CurrencyRepository;
import com.finance.service.DataFetchService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Döviz kurları REST API Controller.
 *
 * <p>
 * Endpoint'ler mimari dokümandaki API listesine uygundur:
 * {@code /api/market/currencies}
 * </p>
 */
@RestController
@RequestMapping("/api/market/currencies")
@CrossOrigin(origins = { "http://localhost:3000", "${finance.cors.allowed-origins:*}" })
public class CurrencyController {

    private static final Logger logger = LogManager.getLogger(CurrencyController.class);

    private final DataFetchService dataFetchService;
    private final CurrencyRepository currencyRepository;

    public CurrencyController(DataFetchService dataFetchService,
            CurrencyRepository currencyRepository) {
        this.dataFetchService = dataFetchService;
        this.currencyRepository = currencyRepository;
    }

    /*
     * ================================================================
     * GET /api/market/currencies
     * Güncel döviz kurları listesi
     * ================================================================
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Currency>>> getTodayRates() {
        logger.info("GET /api/market/currencies – Güncel kurlar istendi");
        List<Currency> rates = dataFetchService.getTodayRates();

        // Bugün için veri yoksa TCMB'den çekmeyi dene
        if (rates.isEmpty()) {
            logger.info("Bugün için kur verisi bulunamadı – TCMB'den çekiliyor...");
            try {
                rates = dataFetchService.fetchAndSaveTcmbRates();
            } catch (Exception e) {
                logger.error("TCMB veri çekme başarısız: {}", e.getMessage());
                return ResponseEntity.ok(ApiResponse.error("Kur verisi alınamadı"));
            }
        }

        return ResponseEntity.ok(ApiResponse.success(rates, rates.size()));
    }

    /*
     * ================================================================
     * GET /api/market/currencies/{code}
     * Belirli bir döviz kodu için güncel kur
     * ================================================================
     */
    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<Currency>> getRateByCode(@PathVariable String code) {
        logger.info("GET /api/market/currencies/{} – Tek kur istendi", code);
        Optional<Currency> rate = currencyRepository
                .findByCurrencyCodeAndDate(code.toUpperCase(), LocalDate.now());

        return rate.map(c -> ResponseEntity.ok(ApiResponse.success(c)))
                .orElse(ResponseEntity.ok(
                        ApiResponse.error("Kur bulunamadı: " + code)));
    }

    /*
     * ================================================================
     * GET /api/market/currencies/{code}/history?from=...&to=...
     * Tarihsel kur verileri (grafik için)
     * ================================================================
     */
    @GetMapping("/{code}/history")
    public ResponseEntity<ApiResponse<List<Currency>>> getHistoricalRates(
            @PathVariable String code,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        logger.info("GET /api/market/currencies/{}/history – {} to {}", code, from, to);
        List<Currency> history = dataFetchService.getHistoricalRates(
                code.toUpperCase(), from, to);

        return ResponseEntity.ok(ApiResponse.success(history, history.size()));
    }

    /*
     * ================================================================
     * POST /api/market/currencies/refresh
     * Manuel kur güncelleme (Admin)
     * ================================================================
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<List<Currency>>> refreshRates() {
        logger.info("POST /api/market/currencies/refresh – Manuel güncelleme");
        try {
            List<Currency> fetched = dataFetchService.fetchAndSaveTcmbRates();
            return ResponseEntity.ok(
                    ApiResponse.success(fetched,"Kurlar başarıyla güncellendi. Toplam:" + fetched.size(),
                            "Kurlar güncellendi"));
        } catch (Exception e) {
            logger.error("Manuel kur güncelleme başarısız: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Güncelleme başarısız: " + e.getMessage()));
        }
    }

    /*
     * ================================================================
     * GET /api/market/currencies/compare?codes=USD,EUR&from=...&to=...
     * Döviz karşılaştırma (çoklu grafik)
     * ================================================================
     */
    @GetMapping("/compare")
    public ResponseEntity<ApiResponse<java.util.Map<String, List<Currency>>>> compareRates(
            @RequestParam List<String> codes,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        logger.info("GET /api/market/currencies/compare – Karşılaştırma: {}", codes);
        java.util.Map<String, List<Currency>> comparison = new java.util.LinkedHashMap<>();

        for (String code : codes) {
            List<Currency> history = dataFetchService.getHistoricalRates(
                    code.toUpperCase().trim(), from, to);
            comparison.put(code.toUpperCase().trim(), history);
        }

        return ResponseEntity.ok(ApiResponse.success(comparison, codes.size()));
    }
}
