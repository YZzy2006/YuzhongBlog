package com.ticketingsystem.yuzhonblog.service;

import com.ticketingsystem.yuzhonblog.common.BusinessException;
import com.ticketingsystem.yuzhonblog.common.ErrorCode;
import com.ticketingsystem.yuzhonblog.entity.AdminUser;
import com.ticketingsystem.yuzhonblog.entity.ContentReview;
import com.ticketingsystem.yuzhonblog.repository.AnnouncementRepository;
import com.ticketingsystem.yuzhonblog.repository.ArticleRepository;
import com.ticketingsystem.yuzhonblog.repository.ContentReviewRepository;
import com.ticketingsystem.yuzhonblog.repository.ProjectRepository;
import com.ticketingsystem.yuzhonblog.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ContentReviewService {

    private final ContentReviewRepository contentReviewRepository;
    private final ArticleRepository articleRepository;
    private final ProjectRepository projectRepository;
    private final AnnouncementRepository announcementRepository;
    private final ArticleService articleService;
    private final ProjectService projectService;
    private final AnnouncementService announcementService;
    private final CurrentUserProvider currentUserProvider;

    @Transactional
    public void approve(Long reviewId, String comment) {
        ContentReview review = contentReviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (!"PENDING".equals(review.getReviewStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "该审核记录已处理");
        }

        AdminUser admin = currentUserProvider.getCurrentUser();
        if (!"super_admin".equals(admin.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(), "仅超级管理员可审核内容");
        }

        boolean contentExists = switch (review.getContentType()) {
            case "ARTICLE" -> articleRepository.existsById(review.getContentId());
            case "PROJECT" -> projectRepository.existsById(review.getContentId());
            case "ANNOUNCEMENT" -> announcementRepository.existsById(review.getContentId());
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(),
                    "未知的内容类型: " + review.getContentType());
        };

        if (!contentExists) {
            review.setReviewStatus("REJECTED");
            review.setReviewedBy(admin.getId());
            review.setReviewedAt(LocalDateTime.now());
            review.setReviewComment("内容已被删除，自动拒绝");
            review.setPendingKey(null);
            contentReviewRepository.save(review);
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "该内容已删除，审核记录已自动拒绝");
        }

        switch (review.getContentType()) {
            case "ARTICLE" -> articleService.updateStatusDirect(review.getContentId(), 1);
            case "PROJECT" -> projectService.publishDirect(review.getContentId());
            case "ANNOUNCEMENT" -> announcementService.activateDirect(review.getContentId());
        }

        review.setReviewStatus("APPROVED");
        review.setPendingKey(null);
        review.setReviewedBy(admin.getId());
        review.setReviewedAt(LocalDateTime.now());
        review.setReviewComment(comment);
        contentReviewRepository.save(review);
    }

    @Transactional
    public void reject(Long reviewId, String comment) {
        ContentReview review = contentReviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (!"PENDING".equals(review.getReviewStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "该审核记录已处理");
        }

        AdminUser admin = currentUserProvider.getCurrentUser();
        if (!"super_admin".equals(admin.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(), "仅超级管理员可审核内容");
        }

        review.setReviewStatus("REJECTED");
        review.setPendingKey(null);
        review.setReviewedBy(admin.getId());
        review.setReviewedAt(LocalDateTime.now());
        review.setReviewComment(comment);
        contentReviewRepository.save(review);
    }
}
