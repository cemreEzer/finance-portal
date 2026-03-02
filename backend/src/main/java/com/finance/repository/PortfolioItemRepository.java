package com.finance.repository;

import com.finance.model.PortfolioItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, Long> {

    /** Portföye ait tüm kalemleri getir */
    List<PortfolioItem> findByPortfolioId(Long portfolioId);

    /** Portföydeki belirli sembolün kalemlerini getir */
    List<PortfolioItem> findByPortfolioIdAndSymbol(Long portfolioId, String symbol);
}
