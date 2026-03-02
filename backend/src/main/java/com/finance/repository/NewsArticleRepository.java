package com.finance.repository;

import com.finance.model.NewsArticle;
import com.finance.model.NewsCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Haber makaleleri için JPA Repository.
 */
@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

    /** Kategoriye göre sayfalı haber listesi */
    Page<NewsArticle> findByCategoryOrderByPublishedAtDesc(NewsCategory category, Pageable pageable);

    /** Tüm haberleri yayın tarihine göre sıralı getir (sayfalı) */
    Page<NewsArticle> findAllByOrderByPublishedAtDesc(Pageable pageable);

    /** Kaynak bazlı haberleri getir */
    Page<NewsArticle> findBySourceOrderByPublishedAtDesc(String source, Pageable pageable);

    /** Tarih aralığındaki haberleri getir */
    Page<NewsArticle> findByPublishedAtBetweenOrderByPublishedAtDesc(
            LocalDateTime from, LocalDateTime to, Pageable pageable);

    /** Başlıkta arama */
    Page<NewsArticle> findByTitleContainingIgnoreCaseOrderByPublishedAtDesc(
            String keyword, Pageable pageable);

    /** External ID ile duplicate kontrolü */
    Optional<NewsArticle> findByExternalId(String externalId);

    /** External ID varlık kontrolü */
    boolean existsByExternalId(String externalId);
}
