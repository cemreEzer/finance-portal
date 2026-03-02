package com.finance.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Portföy kalemi – hisse, döviz, fon vb.
 */
@Entity
@Table(name = "portfolio_items", indexes = {
        @Index(name = "idx_portfolio_item_portfolio", columnList = "portfolio_id"),
        @Index(name = "idx_portfolio_item_symbol", columnList = "symbol")
})
public class PortfolioItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    @JsonIgnore
    private Portfolio portfolio;

    /** Enstrüman tipi: HISSE, DOVIZ, FON, TAHVIL, KRIPTO */
    @NotBlank
    @Column(name = "instrument_type", nullable = false, length = 30)
    private String instrumentType;

    /** Sembol: USD, THYAO, vb. */
    @NotBlank
    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    /** Enstrüman adı */
    @Column(name = "instrument_name", length = 200)
    private String instrumentName;

    /** Miktar */
    @NotNull
    @Positive
    @Column(name = "quantity", precision = 18, scale = 6, nullable = false)
    private BigDecimal quantity;

    /** Alış fiyatı (birim) */
    @NotNull
    @Positive
    @Column(name = "purchase_price", precision = 18, scale = 6, nullable = false)
    private BigDecimal purchasePrice;

    /** Alış tarihi */
    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    /** Notlar */
    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
    public PortfolioItem() {
    }

    /* ─── Getters & Setters ───────────────────────────────────── */
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }

    public void setPortfolio(Portfolio portfolio) {
        this.portfolio = portfolio;
    }

    public String getInstrumentType() {
        return instrumentType;
    }

    public void setInstrumentType(String instrumentType) {
        this.instrumentType = instrumentType;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getInstrumentName() {
        return instrumentName;
    }

    public void setInstrumentName(String instrumentName) {
        this.instrumentName = instrumentName;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
