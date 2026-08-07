package com.ticketingsystem.yuzhonblog.service;

import com.ticketingsystem.yuzhonblog.common.BusinessException;
import com.ticketingsystem.yuzhonblog.common.ErrorCode;
import com.ticketingsystem.yuzhonblog.entity.AdminUser;
import com.ticketingsystem.yuzhonblog.entity.ContentReview;
import com.ticketingsystem.yuzhonblog.repository.ContentReviewRepository;
import com.ticketingsystem.yuzhonblog.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Centralized helper for AI content review logic.
 * Eliminates duplication across ArticleService, ProjectService, AnnouncementService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContentReviewHelper {

    private final AiReviewService aiReviewService;
    private final CurrentUserProvider currentUserProvider;
    private final ContentReviewRepository contentReviewRepository;

    /**
     * Run AI review for non-admin users. Creates a PENDING record if flagged.
     *
     * @return true if content is approved (or user is admin/super_admin)
     * @throws BusinessException REVIEW_PENDING if AI flags the content
     */
    @Transactional
    public boolean reviewAndGate(String contentType, Long contentId, String title, String bodyText) {
        AdminUser currentUser = currentUserProvider.getCurrentUser();
        // 管理员和超级管理员跳过内容审核
        if ("super_admin".equals(currentUser.getRole()) || "admin".equals(currentUser.getRole())) {
            return true;
        }

        checkPendingReview(contentType, contentId);
        AiReviewService.AiReviewResult result = aiReviewService.review(contentType, contentId, title, bodyText);

        if (!result.approved()) {
            createReviewRecord(contentType, contentId, title, currentUser, result.reason());
            throw new BusinessException(ErrorCode.REVIEW_PENDING.getCode(),
                    ErrorCode.REVIEW_PENDING.getMessage() + "：" + result.reason());
        }
        return true;
    }

    /**
     * Check if content has a PENDING review, throw if so.
     * 超级管理员跳过此检查。
     */
    @Transactional(readOnly = true)
    public void checkPendingReview(String contentType, Long contentId) {
        AdminUser currentUser = currentUserProvider.getCurrentUser();
        if ("super_admin".equals(currentUser.getRole())) {
            return;
        }
        contentReviewRepository.findTopByContentTypeAndContentIdAndReviewStatusOrderByCreatedAtDesc(
                contentType, contentId, "PENDING").ifPresent(r -> {
            throw new BusinessException(ErrorCode.REVIEW_PENDING.getCode(),
                    "该内容已有待审核记录，请等待管理员审批");
        });
    }

    @Transactional
    public void deleteReviewsForContent(String contentType, Long contentId) {
        contentReviewRepository.deleteByContentTypeAndContentId(contentType, contentId);
    }

    /**
     * 直接创建待审核记录（普通管理员发布文章时调用，不经过 AI 审核）
     */
    @Transactional
    public void createPendingReview(String contentType, Long contentId, String title, AdminUser user) {
        checkPendingReview(contentType, contentId);
        createReviewRecord(contentType, contentId, title, user, "普通管理员发布，等待超级管理员审核");
    }

    private void createReviewRecord(String contentType, Long contentId, String title,
                                     AdminUser user, String reason) {
        try {
            ContentReview review = new ContentReview();
            review.setContentType(contentType);
            review.setContentId(contentId);
            review.setContentTitle(title);
            review.setSubmittedBy(user.getId());
            review.setSubmittedByName(user.getUsername());
            review.setReviewStatus("PENDING");
            review.setPendingKey("PENDING");
            review.setAiAnalysis(reason);
            contentReviewRepository.save(review);
        } catch (DataIntegrityViolationException e) {
            // Race condition: concurrent request created a PENDING record between check and insert
            log.warn("Duplicate PENDING review detected via DB constraint: contentType={}, contentId={}", contentType, contentId);
            throw new BusinessException(ErrorCode.REVIEW_PENDING.getCode(),
                    "该内容已有待审核记录，请等待管理员审批");
        }
    }
}
