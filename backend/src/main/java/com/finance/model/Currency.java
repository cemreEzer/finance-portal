package com.finance.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Döviz kuru bilgilerini tutan JPA Entity sınıfı.
 * TCMB XML servisinden çekilen veriler bu tabloya kaydedilir.
 */
@Entity
@Table(
    name = "currencies",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"currency_code", "date"},
        name = "uk_currency_code_date"
    ),
    indexes = {
        @Index(name = "idx_currency_code", columnList = "currency_code"),
        @Index(name = "idx_currency_date", columnList = "date")
    }
)
public class Currency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Döviz kodu – örn. USD, EUR, GBP */
    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode;

    /** Döviz adı – örn. "US DOLLAR", "EURO" */
    @Column(name = "currency_name", length = 100)
    private String currencyName;

    /** Döviz alış kuru (Forex Buying) */
    @Column(name = "forex_buying", precision = 18, scale = 6)
    private BigDecimal forexBuying;

    /** Döviz satış kuru (Forex Selling) */
    @Column(name = "forex_selling", precision = 18, scale = 6)
    private BigDecimal forexSelling;

    /** Banknot alış kuru */
    @Column(name = "banknote_buying", precision = 18, scale = 6)
    private BigDecimal banknoteBuying;

    /** Banknot satış kuru */
    @Column(name = "banknote_selling", precision = 18, scale = 6)
    private BigDecimal banknoteSelling;

    /** Birim (1, 100 vb.) */
    @Column(name = "unit", nullable = false)
    private Integer unit = 1;

    /** Kur tarihi */
    @Column(name = "date", nullable = false)
    private LocalDate date;

    /** Veri kaynağı – "TCMB", "BANKA_X" vb. */
    @Column(name = "source", length = 50, nullable = false)
    private String source;

    /** Kayıt oluşturulma zamanı */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Son güncelleme zamanı */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /* ─── Lifecycle Callbacks ─────────────────────────────────── */

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /* ─── Constructors ────────────────────────────────────────── */

    public Currency() {
    }

    public Currency(String currencyCode, String currencyName,
                    BigDecimal forexBuying, BigDecimal forexSelling,
                    BigDecimal banknoteBuying, BigDecimal banknoteSelling,
                    Integer unit, LocalDate date, String source) {
        this.currencyCode = currencyCode;
        this.currencyName = currencyName;
        this.forexBuying = forexBuying;
        this.forexSelling = forexSelling;
        this.banknoteBuying = banknoteBuying;
        this.banknoteSelling = banknoteSelling;
        this.unit = unit;
        this.date = date;
        this.source = source;
    }

    /* ─── Getters & Setters ───────────────────────────────────── */

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public String getCurrencyName() { return currencyName; }
    public void setCurrencyName(String currencyName) { this.currencyName = currencyName; }

    public BigDecimal getForexBuying() { return forexBuying; }
    public void setForexBuying(BigDecimal forexBuying) { this.forexBuying = forexBuying; }

    public BigDecimal getForexSelling() { return forexSelling; }
    public void setForexSelling(BigDecimal forexSelling) { this.forexSelling = forexSelling; }

    public BigDecimal getBanknoteBuying() { return banknoteBuying; }
    public void setBanknoteBuying(BigDecimal banknoteBuying) { this.banknoteBuying = banknoteBuying; }

    public BigDecimal getBanknoteSelling() { return banknoteSelling; }
    public void setBanknoteSelling(BigDecimal banknoteSelling) { this.banknoteSelling = banknoteSelling; }

    public Integer getUnit() { return unit; }
    public void setUnit(Integer unit) { this.unit = unit; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    /* ─── toString ────────────────────────────────────────────── */

    @Override
    public String toString() {
        return "Currency{" +
                "currencyCode='" + currencyCode + '\'' +
                ", forexBuying=" + forexBuying +
                ", forexSelling=" + forexSelling +
                ", date=" + date +
                ", source='" + source + '\'' +
                '}';
    }
}
