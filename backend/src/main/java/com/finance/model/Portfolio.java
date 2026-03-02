package com.finance.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Kullanıcı portföyü JPA Entity.
 * Her portföy bir Keycloak kullanıcısına aittir.
 */
@Entity
@Table(name = "portfolios", indexes = @Index(name = "idx_portfolio_user_id", columnList = "user_id"))
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Keycloak user ID (sub claim) */
    @Column(name = "user_id", nullable = false)
    private String userId;

    /** Portföy adı */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Açıklama */
    @Column(name = "description", length = 500)
    private String description;

    /** Portföy kalemleri */
    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PortfolioItem> items = new ArrayList<>();

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
    public Portfolio() {
    }

    public Portfolio(String userId, String name, String description) {
        this.userId = userId;
        this.name = name;
        this.description = description;
    }

    /* ─── Helpers ──────────────────────────────────────────────── */
    public void addItem(PortfolioItem item) {
        items.add(item);
        item.setPortfolio(this);
    }

    public void removeItem(PortfolioItem item) {
        items.remove(item);
        item.setPortfolio(null);
    }

    /* ─── Getters & Setters ───────────────────────────────────── */
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<PortfolioItem> getItems() {
        return items;
    }

    public void setItems(List<PortfolioItem> items) {
        this.items = items;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
