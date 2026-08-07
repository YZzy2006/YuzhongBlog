package com.ticketingsystem.yuzhonblog.dto.review;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ContentReviewResponse {
    private Long id;
    private String contentType;
    private Long contentId;
    private String contentTitle;
    private String submittedByName;
    private String reviewStatus;
    private String aiAnalysis;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewComment;
    private LocalDateTime createdAt;
}
