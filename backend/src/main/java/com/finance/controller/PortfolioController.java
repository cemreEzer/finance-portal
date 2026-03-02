package com.finance.controller;

import com.finance.dto.ApiResponse;
import com.finance.model.Portfolio;
import com.finance.model.PortfolioItem;
import com.finance.service.PortfolioService;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Portföy yönetimi REST API.
 * Tüm endpoint'ler Keycloak JWT ile korumalıdır.
 * Kullanıcı yalnızca kendi portföylerine erişebilir.
 */
@RestController
@RequestMapping("/api/portfolios")
public class PortfolioController {

    private static final Logger logger = LogManager.getLogger(PortfolioController.class);

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    /** JWT'den Keycloak kullanıcı ID'sini çıkarır */
    private String getUserId(Jwt jwt) {
        return jwt.getSubject(); // Keycloak "sub" claim
    }

    /*
     * ================================================================
     * GET /api/portfolios
     * Kullanıcının tüm portföyleri
     * ================================================================
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Portfolio>>> getPortfolios(
            @AuthenticationPrincipal Jwt jwt) {
        String userId = getUserId(jwt);
        logger.info("GET /api/portfolios – kullanıcı: {}", userId);
        List<Portfolio> portfolios = portfolioService.getUserPortfolios(userId);
        return ResponseEntity.ok(ApiResponse.success(portfolios, portfolios.size()));
    }

    /*
     * ================================================================
     * GET /api/portfolios/{id}
     * Portföy detay
     * ================================================================
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Portfolio>> getPortfolio(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = getUserId(jwt);
        logger.info("GET /api/portfolios/{} – kullanıcı: {}", id, userId);
        Portfolio portfolio = portfolioService.getPortfolioByIdAndUser(id, userId);
        return ResponseEntity.ok(ApiResponse.success(portfolio));
    }

    /*
     * ================================================================
     * POST /api/portfolios
     * Yeni portföy oluştur
     * ================================================================
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Portfolio>> createPortfolio(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = getUserId(jwt);
        String name = body.getOrDefault("name", "Portföyüm");
        String description = body.get("description");
        logger.info("POST /api/portfolios – {} – kullanıcı: {}", name, userId);
        Portfolio portfolio = portfolioService.createPortfolio(userId, name, description);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(portfolio, "Portföy oluşturuldu"));
    }

    /*
     * ================================================================
     * PUT /api/portfolios/{id}
     * Portföy güncelle
     * ================================================================
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Portfolio>> updatePortfolio(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = getUserId(jwt);
        logger.info("PUT /api/portfolios/{} – kullanıcı: {}", id, userId);
        Portfolio updated = portfolioService.updatePortfolio(
                id, userId, body.get("name"), body.get("description"));
        return ResponseEntity.ok(ApiResponse.success(updated, "Portföy güncellendi"));
    }

    /*
     * ================================================================
     * DELETE /api/portfolios/{id}
     * Portföy sil
     * ================================================================
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePortfolio(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = getUserId(jwt);
        logger.info("DELETE /api/portfolios/{} – kullanıcı: {}", id, userId);
        portfolioService.deletePortfolio(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Portföy silindi"));
    }

    /*
     * ================================================================
     * POST /api/portfolios/{id}/items
     * Portföye enstrüman ekle
     * ================================================================
     */
    @PostMapping("/{id}/items")
    public ResponseEntity<ApiResponse<PortfolioItem>> addItem(
            @PathVariable Long id,
            @Valid @RequestBody PortfolioItem item,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = getUserId(jwt);
        logger.info("POST /api/portfolios/{}/items – {} {} – kullanıcı: {}",
                id, item.getQuantity(), item.getSymbol(), userId);
        PortfolioItem saved = portfolioService.addItem(id, userId, item);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(saved, "Enstrüman eklendi"));
    }

    /*
     * ================================================================
     * DELETE /api/portfolios/{id}/items/{itemId}
     * Enstrüman sil
     * ================================================================
     */
    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> removeItem(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = getUserId(jwt);
        logger.info("DELETE /api/portfolios/{}/items/{} – kullanıcı: {}", id, itemId, userId);
        portfolioService.removeItem(id, itemId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Enstrüman silindi"));
    }

    /*
     * ================================================================
     * GET /api/portfolios/{id}/summary
     * Portföy özeti – kâr/zarar, dağılım
     * ================================================================
     */
    @GetMapping("/{id}/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSummary(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = getUserId(jwt);
        logger.info("GET /api/portfolios/{}/summary – kullanıcı: {}", id, userId);
        Map<String, Object> summary = portfolioService.getPortfolioSummary(id, userId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
