package com.ticketingsystem.yuzhonblog.repository;

import com.ticketingsystem.yuzhonblog.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long>, ArticleRepositoryCustom {

    @Query("SELECT DISTINCT a FROM Article a LEFT JOIN FETCH a.category WHERE a.status = :status ORDER BY a.isTop DESC, a.createdAt DESC")
    Page<Article> findPublishedWithCategory(@Param("status") Integer status, Pageable pageable);

    @Query("SELECT DISTINCT a FROM Article a LEFT JOIN FETCH a.category WHERE a.status = :status ORDER BY a.viewCount DESC")
    Page<Article> findPublishedPopular(@Param("status") Integer status, Pageable pageable);

    @Query("SELECT DISTINCT a FROM Article a LEFT JOIN FETCH a.category WHERE a.status = :status AND a.isFeatured = 1 ORDER BY a.isTop DESC, a.createdAt DESC")
    Page<Article> findPublishedFeatured(@Param("status") Integer status, Pageable pageable);

    @Query("SELECT DISTINCT a FROM Article a LEFT JOIN FETCH a.category WHERE a.status = :status AND a.category.id = :categoryId ORDER BY a.isTop DESC, a.createdAt DESC")
    Page<Article> findPublishedWithCategoryByCategoryId(@Param("status") Integer status, @Param("categoryId") Long categoryId, Pageable pageable);

    @Query("SELECT DISTINCT a FROM Article a LEFT JOIN FETCH a.category WHERE a.status = :status ORDER BY a.isTop DESC, a.createdAt DESC")
    Page<Article> findByStatusOrderByIsTopDescCreatedAtDesc(@Param("status") Integer status, Pageable pageable);

    @Query("SELECT DISTINCT a FROM Article a LEFT JOIN FETCH a.category WHERE a.status = :status AND a.category.id = :categoryId ORDER BY a.isTop DESC, a.createdAt DESC")
    Page<Article> findByStatusAndCategoryIdOrderByIsTopDescCreatedAtDesc(@Param("status") Integer status, @Param("categoryId") Long categoryId, Pageable pageable);

    @Query("SELECT DISTINCT a FROM Article a LEFT JOIN FETCH a.category WHERE a.category.id = :categoryId ORDER BY a.isTop DESC, a.createdAt DESC")
    Page<Article> findByCategoryIdOrderByIsTopDescCreatedAtDesc(@Param("categoryId") Long categoryId, Pageable pageable);

    @Query("SELECT DISTINCT a FROM Article a LEFT JOIN FETCH a.category WHERE a.status = :status AND (a.title LIKE %:keyword% OR a.summary LIKE %:keyword%) ORDER BY a.createdAt DESC")
    Page<Article> searchByKeyword(@Param("status") Integer status, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT DISTINCT a FROM Article a LEFT JOIN FETCH a.category WHERE a.title LIKE %:keyword% OR a.summary LIKE %:keyword% ORDER BY a.createdAt DESC")
    Page<Article> searchByKeywordAllStatuses(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT DISTINCT a FROM Article a LEFT JOIN FETCH a.category WHERE a.status = :status AND (a.title LIKE %:keyword% OR a.summary LIKE %:keyword%) ORDER BY a.isTop DESC, a.createdAt DESC")
    Page<Article> searchPublishedByKeyword(@Param("status") Integer status, @Param("keyword") String keyword, Pageable pageable);

    // Level 2: search title + summary + content
    @Query("SELECT DISTINCT a FROM Article a LEFT JOIN FETCH a.category WHERE a.status = :status AND (a.title LIKE %:keyword% OR a.summary LIKE %:keyword% OR a.contentMd LIKE %:keyword%) ORDER BY a.isTop DESC, a.createdAt DESC")
    Page<Article> searchPublishedByKeywordDeep(@Param("status") Integer status, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT COUNT(a) FROM Article a WHERE a.category.id = :categoryId AND a.status = 1")
    long countPublishedByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT COUNT(a) FROM Article a WHERE a.category.id = :categoryId")
    long countByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT a.category.id, COUNT(a) FROM Article a WHERE a.category.id IN :categoryIds AND a.status = 1 GROUP BY a.category.id")
    List<Object[]> countPublishedByCategoryIds(@Param("categoryIds") List<Long> categoryIds);

    @Query("SELECT a FROM Article a LEFT JOIN FETCH a.category WHERE a.slug = :slug")
    Optional<Article> findBySlug(@Param("slug") String slug);

    boolean existsBySlug(String slug);

    @Query("SELECT a.id FROM Article a WHERE a.slug = :slug")
    Optional<Long> findIdBySlug(@Param("slug") String slug);

    @Query("SELECT a.likeCount FROM Article a WHERE a.id = :id")
    Optional<Integer> findLikeCountById(@Param("id") Long id);

    @Query("SELECT DISTINCT a FROM Article a LEFT JOIN FETCH a.category ORDER BY a.isTop DESC, a.createdAt DESC")
    Page<Article> findAllWithCategory(Pageable pageable);

    @Query("SELECT COUNT(a) FROM Article a WHERE a.status = :status")
    long countByStatus(@Param("status") Integer status);

    @Query("SELECT DISTINCT a FROM Article a LEFT JOIN FETCH a.category JOIN ArticleTag at ON at.article.id = a.id WHERE at.tag.id = :tagId ORDER BY a.isTop DESC, a.createdAt DESC")
    Page<Article> findByTagId(@Param("tagId") Long tagId, Pageable pageable);

    @Query("SELECT DISTINCT a FROM Article a LEFT JOIN FETCH a.category JOIN ArticleTag at ON at.article.id = a.id WHERE a.status = :status AND at.tag.id = :tagId ORDER BY a.isTop DESC, a.createdAt DESC")
    Page<Article> findByStatusAndTagId(@Param("status") Integer status, @Param("tagId") Long tagId, Pageable pageable);

    @Modifying
    @Query("UPDATE Article a SET a.viewCount = a.viewCount + 1 WHERE a.id = :id")
    int incrementViewCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Article a SET a.likeCount = COALESCE(a.likeCount, 0) + 1 WHERE a.id = :id")
    int incrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Article a SET a.likeCount = GREATEST(COALESCE(a.likeCount, 0) - 1, 0) WHERE a.id = :id")
    int decrementLikeCount(@Param("id") Long id);

    // Previous article (older)
    @Query(value = "SELECT * FROM article WHERE status = 1 AND created_at < :createdAt ORDER BY created_at DESC LIMIT 1", nativeQuery = true)
    Article findPrevious(@Param("createdAt") java.time.LocalDateTime createdAt);

    // Next article (newer)
    @Query(value = "SELECT * FROM article WHERE status = 1 AND created_at > :createdAt ORDER BY created_at ASC LIMIT 1", nativeQuery = true)
    Article findNext(@Param("createdAt") java.time.LocalDateTime createdAt);

    // Related articles: same category, exclude current, limit 4
    @Query("SELECT a FROM Article a LEFT JOIN FETCH a.category WHERE a.status = 1 AND a.category.id = :categoryId AND a.id != :excludeId ORDER BY a.createdAt DESC")
    List<Article> findRelated(@Param("categoryId") Long categoryId, @Param("excludeId") Long excludeId, Pageable pageable);

    // Related articles fallback: recent articles, exclude current, limit 4
    @Query("SELECT a FROM Article a LEFT JOIN FETCH a.category WHERE a.status = 1 AND a.id != :excludeId ORDER BY a.createdAt DESC")
    List<Article> findRecentExclude(@Param("excludeId") Long excludeId, Pageable pageable);

    // Date range queries
    @Query("SELECT DISTINCT a FROM Article a LEFT JOIN FETCH a.category WHERE a.status = :status AND a.createdAt BETWEEN :start AND :end ORDER BY a.isTop DESC, a.createdAt DESC")
    Page<Article> findPublishedWithDateRange(@Param("status") Integer status, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @Query("SELECT DISTINCT a FROM Article a LEFT JOIN FETCH a.category WHERE a.status = :status AND a.category.id = :categoryId AND a.createdAt BETWEEN :start AND :end ORDER BY a.isTop DESC, a.createdAt DESC")
    Page<Article> findPublishedWithCategoryAndDateRange(@Param("status") Integer status, @Param("categoryId") Long categoryId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @Query("SELECT DISTINCT a FROM Article a LEFT JOIN FETCH a.category JOIN ArticleTag at ON at.article.id = a.id WHERE a.status = :status AND at.tag.id = :tagId AND a.createdAt BETWEEN :start AND :end ORDER BY a.isTop DESC, a.createdAt DESC")
    Page<Article> findPublishedWithTagAndDateRange(@Param("status") Integer status, @Param("tagId") Long tagId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @Query("SELECT DISTINCT a FROM Article a LEFT JOIN FETCH a.category WHERE a.status = :status AND (a.title LIKE %:keyword% OR a.summary LIKE %:keyword% OR a.contentMd LIKE %:keyword%) AND a.createdAt BETWEEN :start AND :end ORDER BY a.isTop DESC, a.createdAt DESC")
    Page<Article> searchPublishedByKeywordWithDateRange(@Param("status") Integer status, @Param("keyword") String keyword, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @Query("SELECT DISTINCT a FROM Article a LEFT JOIN FETCH a.category WHERE a.status = :status AND a.isFeatured = 1 AND a.createdAt BETWEEN :start AND :end ORDER BY a.isTop DESC, a.createdAt DESC")
    Page<Article> findPublishedFeaturedWithDateRange(@Param("status") Integer status, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @Query("SELECT DISTINCT a FROM Article a LEFT JOIN FETCH a.category WHERE a.status = :status AND a.createdAt BETWEEN :start AND :end ORDER BY a.viewCount DESC")
    Page<Article> findPublishedPopularWithDateRange(@Param("status") Integer status, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @Query("SELECT a FROM Article a WHERE a.status = :status AND (a.titleEn IS NULL OR a.titleEn = '' OR a.summaryEn IS NULL OR a.summaryEn = '' OR a.contentMdEn IS NULL OR a.contentMdEn = '')")
    List<Article> findNeedingTranslation(@Param("status") Integer status);

    @Query("SELECT a.id FROM Article a WHERE a.status = :status AND (a.titleEn IS NULL OR a.titleEn = '' OR a.summaryEn IS NULL OR a.summaryEn = '' OR a.contentMdEn IS NULL OR a.contentMdEn = '')")
    List<Long> findIdsNeedingTranslation(@Param("status") Integer status);
}
