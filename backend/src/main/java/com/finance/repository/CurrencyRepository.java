package com.finance.repository;

import com.finance.model.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Döviz kuru verileri için JPA Repository.
 */
@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Long> {

    /** Belirli bir tarih için tüm kurları getir */
    List<Currency> findByDate(LocalDate date);

    /** Belirli bir döviz kodu ve tarih için kur getir */
    Optional<Currency> findByCurrencyCodeAndDate(String currencyCode, LocalDate date);

    /**
     * Belirli bir döviz kodu için tarih aralığındaki verileri getir (tarihsel
     * analiz)
     */
    List<Currency> findByCurrencyCodeAndDateBetweenOrderByDateAsc(
            String currencyCode, LocalDate startDate, LocalDate endDate);

    /** Belirli bir kaynak için en son tarihteki verileri getir */
    List<Currency> findBySourceAndDate(String source, LocalDate date);

    /**
     * Belirli bir döviz kodu ve kaynak için kaydın var olup olmadığını kontrol et
     */
    boolean existsByCurrencyCodeAndDateAndSource(String currencyCode, LocalDate date, String source);
}
