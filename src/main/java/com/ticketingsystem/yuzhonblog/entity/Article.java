package com.ticketingsystem.yuzhonblog.entity;

import com.ticketingsystem.yuzhonblog.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.BatchSize;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@BatchSize(size = 20)
@Table(name = "article", indexes = {
    @Index(name = "idx_article_status_top_created", columnList = "status, is_top, created_at"),
    @Index(name = "idx_article_status_featured_top", columnList = "status, is_featured, is_top, created_at"),
    @Index(name = "idx_article_status_views", columnList = "status, view_count"),
    @Index(name = "idx_article_category_status", columnList = "category_id, status"),
    @Index(name = "idx_article_slug", columnList = "slug")
})
public class Article extends BaseEntity {

    @Version
    private Long version;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "title_en", length = 200)
    private String titleEn;

    @Column(name = "content_md", columnDefinition = "LONGTEXT")
    private String contentMd;

    @Column(name = "content_md_en", columnDefinition = "LONGTEXT")
    private String contentMdEn;

    @Column(name = "content_html", columnDefinition = "LONGTEXT")
    private String contentHtml;

    @Column(name = "content_html_en", columnDefinition = "LONGTEXT")
    private String contentHtmlEn;

    @Column(length = 500)
    private String summary;

    @Column(name = "summary_en", length = 500)
    private String summaryEn;

    @Column(name = "cover_image", length = 500)
    private String coverImage;

    @Column(unique = true, length = 200)
    private String slug;

    @Column(nullable = false)
    private Integer status = 0; // 0=draft, 1=published, 2=archived, 3=pending_review

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Column(name = "like_count")
    private Integer likeCount = 0;

    @Column(name = "is_top")
    private Integer isTop = 0;

    @Column(name = "is_featured")
    private Integer isFeatured = 0;

    @Column(name = "card_style")
    private Integer cardStyle = 0;

    @Column(name = "author_notes", columnDefinition = "TEXT")
    private String authorNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
}
