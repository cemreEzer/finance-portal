package com.finance.repository;

import com.finance.model.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    /** Kullanıcının tüm portföylerini getir */
    List<Portfolio> findByUserId(String userId);

    /** Belirli portföyü kullanıcı kontrolüyle getir */
    Optional<Portfolio> findByIdAndUserId(Long id, String userId);

    /** Kullanıcının portföy sayısı */
    long countByUserId(String userId);
}
