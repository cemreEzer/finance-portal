package com.finance.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Haber makaleleri için JPA Entity sınıfı.
 * Dış kaynaklardan (RSS, API) çekilen haberler bu tabloya kaydedilir.
 */
@Entity
@Table(name = "news_articles", indexes = {
        @Index(name = "idx_news_category", columnList = "category"),
        @Index(name = "idx_news_published_at", columnList = "published_at"),
        @Index(name = "idx_news_source", columnList = "source")
})
public class NewsArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Haber başlığı */
    @Column(name = "title", nullable = false, length = 500)
    private String title;

    /** Haber özeti / içerik özeti */
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    /** Haber içeriğinin tamamı (opsiyonel) */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /** Haber kaynağı – "Bloomberg HT", "Reuters", "Hürriyet" vb. */
    @Column(name = "source", length = 100)
    private String source;

    /** Kaynak URL */
    @Column(name = "url", length = 1000)
    private String url;

    /** Haber görseli URL (opsiyonel) */
    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    /** Haber kategorisi */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 30, nullable = false)
    private NewsCategory category = NewsCategory.GENEL_EKONOMI;

    /** Haberin yayınlanma tarihi */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    /** Kayıt oluşturulma zamanı */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Benzersiz haber tanımlayıcı (duplicate kontrolü için) */
    @Column(name = "external_id", length = 500, unique = true)
    private String externalId;

    /* ─── Lifecycle Callbacks ─────────────────────────────────── */

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /* ─── Constructors ────────────────────────────────────────── */

    public NewsArticle() {
    }

    public NewsArticle(String title, String summary, String source,
            String url, NewsCategory category, LocalDateTime publishedAt) {
        this.title = title;
        this.summary = summary;
        this.source = source;
        this.url = url;
        this.category = category;
        this.publishedAt = publishedAt;
    }

    /* ─── Getters & Setters ───────────────────────────────────── */

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public NewsCategory getCategory() {
        return category;
    }

    public void setCategory(NewsCategory category) {
        this.category = category;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    @Override
    public String toString() {
        return "NewsArticle{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", category=" + category +
                ", source='" + source + '\'' +
                ", publishedAt=" + publishedAt +
                '}';
    }
}
