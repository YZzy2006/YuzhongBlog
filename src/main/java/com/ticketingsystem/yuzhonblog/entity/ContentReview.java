package com.ticketingsystem.yuzhonblog.entity;

import com.ticketingsystem.yuzhonblog.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "content_review",
        indexes = {
            @Index(columnList = "content_type,content_id,review_status"),
            @Index(name = "idx_review_status_time", columnList = "review_status, created_at")
        },
        uniqueConstraints = @UniqueConstraint(columnNames = {"content_type", "content_id", "pending_key"}))
public class ContentReview extends BaseEntity {

    @Column(name = "content_type", nullable = false, length = 30)
    private String contentType;  // "ARTICLE", "PROJECT", "ANNOUNCEMENT"

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(name = "content_title", length = 200)
    private String contentTitle;

    @Column(name = "submitted_by", nullable = false)
    private Long submittedBy;

    @Column(name = "submitted_by_name", length = 50)
    private String submittedByName;

    @Column(name = "review_status", nullable = false, length = 20)
    private String reviewStatus;  // "PENDING", "APPROVED", "REJECTED"

    // Non-null only when reviewStatus=PENDING; enforces one-PENDING-per-content uniqueness
    @Column(name = "pending_key", length = 20)
    private String pendingKey;

    @Column(name = "ai_analysis", columnDefinition = "TEXT")
    private String aiAnalysis;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_comment", length = 500)
    private String reviewComment;
}
