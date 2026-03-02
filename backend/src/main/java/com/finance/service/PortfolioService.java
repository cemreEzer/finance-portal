package com.finance.service;

import com.finance.exception.ResourceNotFoundException;
import com.finance.model.Currency;
import com.finance.model.Portfolio;
import com.finance.model.PortfolioItem;
import com.finance.repository.CurrencyRepository;
import com.finance.repository.PortfolioItemRepository;
import com.finance.repository.PortfolioRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Portföy yönetimi servisi – CRUD + kâr/zarar hesaplama.
 */
@Service
public class PortfolioService {

    private static final Logger logger = LogManager.getLogger(PortfolioService.class);

    private final PortfolioRepository portfolioRepository;
    private final PortfolioItemRepository itemRepository;
    private final CurrencyRepository currencyRepository;

    public PortfolioService(PortfolioRepository portfolioRepository,
            PortfolioItemRepository itemRepository,
            CurrencyRepository currencyRepository) {
        this.portfolioRepository = portfolioRepository;
        this.itemRepository = itemRepository;
        this.currencyRepository = currencyRepository;
    }

    /*
     * ================================================================
     * PORTFÖY CRUD
     * ================================================================
     */

    /** Kullanıcının tüm portföylerini getir */
    public List<Portfolio> getUserPortfolios(String userId) {
        return portfolioRepository.findByUserId(userId);
    }

    /** Portföy detay (sahiplik kontrolüyle) */
    public Portfolio getPortfolioByIdAndUser(Long id, String userId) {
        return portfolioRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Portföy bulunamadı: ID " + id));
    }

    /** Yeni portföy oluştur */
    @Transactional
    public Portfolio createPortfolio(String userId, String name, String description) {
        logger.info("Yeni portföy oluşturuluyor: {} – kullanıcı: {}", name, userId);
        Portfolio portfolio = new Portfolio(userId, name, description);
        return portfolioRepository.save(portfolio);
    }

    /** Portföy güncelle */
    @Transactional
    public Portfolio updatePortfolio(Long id, String userId, String name, String description) {
        Portfolio portfolio = getPortfolioByIdAndUser(id, userId);
        portfolio.setName(name);
        if (description != null)
            portfolio.setDescription(description);
        return portfolioRepository.save(portfolio);
    }

    /** Portföy sil */
    @Transactional
    public void deletePortfolio(Long id, String userId) {
        Portfolio portfolio = getPortfolioByIdAndUser(id, userId);
        portfolioRepository.delete(portfolio);
        logger.info("Portföy silindi: ID {} – kullanıcı: {}", id, userId);
    }

    /*
     * ================================================================
     * PORTFÖY KALEMİ CRUD
     * ================================================================
     */

    /** Portföye enstrüman ekle */
    @Transactional
    public PortfolioItem addItem(Long portfolioId, String userId, PortfolioItem item) {
        Portfolio portfolio = getPortfolioByIdAndUser(portfolioId, userId);
        portfolio.addItem(item);
        portfolioRepository.save(portfolio);
        logger.info("Enstrüman eklendi: {} {} – portföy: {}",
                item.getQuantity(), item.getSymbol(), portfolioId);
        return item;
    }

    /** Portföyden enstrüman sil */
    @Transactional
    public void removeItem(Long portfolioId, Long itemId, String userId) {
        Portfolio portfolio = getPortfolioByIdAndUser(portfolioId, userId);
        PortfolioItem item = portfolio.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Enstrüman bulunamadı: ID " + itemId));
        portfolio.removeItem(item);
        portfolioRepository.save(portfolio);
        logger.info("Enstrüman silindi: ID {} – portföy: {}", itemId, portfolioId);
    }

    /*
     * ================================================================
     * PORTFÖY ÖZETİ – KÂR/ZARAR HESAPLAMA
     * ================================================================
     */

    /**
     * Portföy özeti: güncel değer, toplam maliyet, kâr/zarar, dağılım.
     */
    public Map<String, Object> getPortfolioSummary(Long portfolioId, String userId) {
        Portfolio portfolio = getPortfolioByIdAndUser(portfolioId, userId);
        List<PortfolioItem> items = portfolio.getItems();

        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalCurrentValue = BigDecimal.ZERO;
        List<Map<String, Object>> itemDetails = new ArrayList<>();
        Map<String, BigDecimal> typeDistribution = new LinkedHashMap<>();

        for (PortfolioItem item : items) {
            BigDecimal cost = item.getQuantity().multiply(item.getPurchasePrice());
            BigDecimal currentPrice = getCurrentPrice(item.getSymbol(), item.getInstrumentType());
            BigDecimal currentValue = item.getQuantity().multiply(currentPrice);
            BigDecimal pnl = currentValue.subtract(cost);
            BigDecimal pnlPercent = cost.compareTo(BigDecimal.ZERO) > 0
                    ? pnl.divide(cost, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;

            totalCost = totalCost.add(cost);
            totalCurrentValue = totalCurrentValue.add(currentValue);

            // Tip bazlı dağılım
            typeDistribution.merge(item.getInstrumentType(), currentValue, BigDecimal::add);

            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("id", item.getId());
            detail.put("symbol", item.getSymbol());
            detail.put("instrumentType", item.getInstrumentType());
            detail.put("instrumentName", item.getInstrumentName());
            detail.put("quantity", item.getQuantity());
            detail.put("purchasePrice", item.getPurchasePrice());
            detail.put("currentPrice", currentPrice);
            detail.put("cost", cost.setScale(2, RoundingMode.HALF_UP));
            detail.put("currentValue", currentValue.setScale(2, RoundingMode.HALF_UP));
            detail.put("pnl", pnl.setScale(2, RoundingMode.HALF_UP));
            detail.put("pnlPercent", pnlPercent.setScale(2, RoundingMode.HALF_UP));

            itemDetails.add(detail);
        }

        BigDecimal totalPnl = totalCurrentValue.subtract(totalCost);
        BigDecimal totalPnlPercent = totalCost.compareTo(BigDecimal.ZERO) > 0
                ? totalPnl.divide(totalCost, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("portfolioId", portfolio.getId());
        summary.put("portfolioName", portfolio.getName());
        summary.put("totalCost", totalCost.setScale(2, RoundingMode.HALF_UP));
        summary.put("totalCurrentValue", totalCurrentValue.setScale(2, RoundingMode.HALF_UP));
        summary.put("totalPnl", totalPnl.setScale(2, RoundingMode.HALF_UP));
        summary.put("totalPnlPercent", totalPnlPercent.setScale(2, RoundingMode.HALF_UP));
        summary.put("itemCount", items.size());
        summary.put("distribution", typeDistribution);
        summary.put("items", itemDetails);

        return summary;
    }

    /**
     * Enstrümanın güncel fiyatını al.
     * Döviz: currencies tablosundan, diğer: alış fiyatı fallback.
     */
    private BigDecimal getCurrentPrice(String symbol, String instrumentType) {
        if ("DOVIZ".equalsIgnoreCase(instrumentType)) {
            Optional<Currency> currency = currencyRepository
                    .findByCurrencyCodeAndDate(symbol, LocalDate.now());
            if (currency.isPresent() && currency.get().getForexSelling() != null) {
                return currency.get().getForexSelling();
            }
        }
        // Fallback: Döviz dışındaki enstrümanlar veya veri yoksa
        // Gerçek implementasyonda hisse/fon API'sinden çekilecek
        logger.debug("Güncel fiyat bulunamadı: {} ({}) – fallback", symbol, instrumentType);
        return BigDecimal.ZERO;
    }
}
