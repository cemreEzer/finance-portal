package com.finance.controller;

import com.finance.dto.ApiResponse;
import com.finance.model.NewsArticle;
import com.finance.model.NewsCategory;
import com.finance.service.NewsFetchService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Haber modülü REST API Controller.
 *
 * <p>
 * Doküman isterleri:
 * <ul>
 * <li>Haber listesi (sayfalı)</li>
 * <li>Haber detay sayfası</li>
 * <li>Kategoriye göre filtreleme</li>
 * <li>Kategori listesi</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/news")
@CrossOrigin(origins = { "http://localhost:3000", "${finance.cors.allowed-origins:*}" })
public class NewsController {

    private static final Logger logger = LogManager.getLogger(NewsController.class);

    private final NewsFetchService newsFetchService;

    public NewsController(NewsFetchService newsFetchService) {
        this.newsFetchService = newsFetchService;
    }

    /*
     * ================================================================
     * GET /api/news?page=0&size=20
     * Haber listesi (sayfalı, en yeni önce)
     * ================================================================
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        logger.info("GET /api/news – sayfa: {}, boyut: {}", page, size);
        Page<NewsArticle> newsPage = newsFetchService.getLatestNews(page, size);

        Map<String, Object> data = Map.of(
                "articles", newsPage.getContent(),
                "currentPage", newsPage.getNumber(),
                "totalPages", newsPage.getTotalPages(),
                "totalItems", newsPage.getTotalElements());

        return ResponseEntity.ok(ApiResponse.success(data, (int) newsPage.getTotalElements()));
    }

    /*
     * ================================================================
     * GET /api/news/{id}
     * Haber detay sayfası
     * ================================================================
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NewsArticle>> getNewsById(@PathVariable Long id) {
        logger.info("GET /api/news/{} – Haber detay istendi", id);
        return newsFetchService.getNewsById(id)
                .map(article -> ResponseEntity.ok(ApiResponse.success(article)))
                .orElse(ResponseEntity.ok(
                        ApiResponse.error("Haber bulunamadı: ID " + id)));
    }

    /*
     * ================================================================
     * GET /api/news/categories
     * Kategori listesi
     * ================================================================
     */
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getCategories() {
        logger.info("GET /api/news/categories – Kategori listesi istendi");

        List<Map<String, String>> categories = newsFetchService.getCategories().stream()
                .map(c -> Map.of(
                        "name", c.name(),
                        "displayName", c.getDisplayName()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(categories, categories.size()));
    }

    /*
     * ================================================================
     * GET /api/news/category/{category}?page=0&size=20
     * Kategoriye göre haber filtreleme
     * ================================================================
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNewsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        logger.info("GET /api/news/category/{} – Kategoriye göre filtreleme", category);

        NewsCategory newsCategory;
        try {
            newsCategory = NewsCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Geçersiz kategori: " + category));
        }

        Page<NewsArticle> newsPage = newsFetchService.getNewsByCategory(newsCategory, page, size);

        Map<String, Object> data = Map.of(
                "articles", newsPage.getContent(),
                "category", newsCategory.getDisplayName(),
                "currentPage", newsPage.getNumber(),
                "totalPages", newsPage.getTotalPages(),
                "totalItems", newsPage.getTotalElements());

        return ResponseEntity.ok(ApiResponse.success(data, (int) newsPage.getTotalElements()));
    }

    /*
     * ================================================================
     * GET /api/news/search?q=keyword&page=0&size=20
     * Haber arama
     * ================================================================
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchNews(
            @RequestParam("q") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        logger.info("GET /api/news/search?q={} – Haber arama", keyword);
        Page<NewsArticle> newsPage = newsFetchService.searchNews(keyword, page, size);

        Map<String, Object> data = Map.of(
                "articles", newsPage.getContent(),
                "query", keyword,
                "currentPage", newsPage.getNumber(),
                "totalPages", newsPage.getTotalPages(),
                "totalItems", newsPage.getTotalElements());

        return ResponseEntity.ok(ApiResponse.success(data, (int) newsPage.getTotalElements()));
    }

    /*
     * ================================================================
     * POST /api/news/refresh
     * Manuel haber güncelleme (Admin)
     * ================================================================
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refreshNews() {
        logger.info("POST /api/news/refresh – Manuel haber güncelleme");
        try {
            newsFetchService.scheduledNewsFetch();
            return ResponseEntity.ok(
                    ApiResponse.success("Haberler güncellendi", "Güncelleme başarılı"));
        } catch (Exception e) {
            logger.error("Manuel haber güncelleme başarısız: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Güncelleme başarısız: " + e.getMessage()));
        }
    }
}
