package com.ticketingsystem.yuzhonblog.service;

import com.ticketingsystem.yuzhonblog.common.BusinessException;
import com.ticketingsystem.yuzhonblog.common.ErrorCode;
import com.ticketingsystem.yuzhonblog.dto.announcement.AnnouncementCreateRequest;
import com.ticketingsystem.yuzhonblog.dto.PageResult;
import com.ticketingsystem.yuzhonblog.dto.announcement.AnnouncementResponse;
import com.ticketingsystem.yuzhonblog.dto.announcement.AnnouncementUpdateRequest;
import com.ticketingsystem.yuzhonblog.entity.Announcement;
import com.ticketingsystem.yuzhonblog.repository.AnnouncementRepository;
import com.ticketingsystem.yuzhonblog.security.CurrentUserProvider;
import com.ticketingsystem.yuzhonblog.util.HtmlSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final ContentReviewHelper contentReviewHelper;
    private final CurrentUserProvider currentUserProvider;
    private final AnnouncementTranslationService announcementTranslationService;
    private final HtmlSanitizer htmlSanitizer;

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getPublicList() {
        return announcementRepository.findByActiveTrueOrderBySortOrderAscCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PageResult<AnnouncementResponse> getPublicPage(int page, int size, String keyword, String type, String tag, String sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        var pageable = buildPageable(safePage, safeSize, sort);
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        Page<Announcement> result = announcementRepository.findPublicFiltered(kw, type, tag, pageable);
        return PageResult.of(result.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getAdminList() {
        return announcementRepository.findAllByOrderBySortOrderAscCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PageResult<AnnouncementResponse> getAdminPage(int page, int size, String keyword, String type, Boolean active, String sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        var pageable = buildPageable(safePage, safeSize, sort);
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        String t = (type != null && !type.isBlank()) ? type : null;
        Page<Announcement> result = announcementRepository.findAdminFiltered(kw, t, active, pageable);
        return PageResult.of(result.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public AnnouncementResponse getById(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANNOUNCEMENT_NOT_FOUND));
        return toResponse(announcement);
    }

    @Transactional
    public AnnouncementResponse create(AnnouncementCreateRequest request) {
        checkSensitiveContent(request.getTag(), request.getTitle(), request.getContent());

        // Save as inactive first so we have a real ID for review records
        Announcement announcement = new Announcement();
        announcement.setTag(request.getTag());
        announcement.setTagEn(request.getTagEn());
        announcement.setType(request.getType());
        announcement.setTitle(request.getTitle());
        announcement.setTitleEn(request.getTitleEn());
        announcement.setContent(htmlSanitizer.sanitize(request.getContent()));
        announcement.setContentEn(htmlSanitizer.sanitize(request.getContentEn()));
        announcement.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        announcement.setLevel(request.getLevel() != null ? request.getLevel() : "info");
        announcement.setDisplayStyle(request.getDisplayStyle() != null ? request.getDisplayStyle() : "banner");
        announcement.setActive(false);
        announcement = announcementRepository.save(announcement);

        // AI review for non-super_admin (throws REVIEW_PENDING if flagged)
        contentReviewHelper.reviewAndGate("ANNOUNCEMENT", announcement.getId(),
                request.getTitle(), request.getContent());

        announcement.setActive(true);
        announcement = announcementRepository.save(announcement);

        // Auto-translate if English fields are missing
        if (needsTranslation(announcement)) {
            announcementTranslationService.translateAnnouncement(announcement.getId());
        }

        return toResponse(announcement);
    }

    @Transactional
    public AnnouncementResponse update(Long id, AnnouncementUpdateRequest request) {
        checkSensitiveContent(request.getTag(), request.getTitle(), request.getContent());
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANNOUNCEMENT_NOT_FOUND));

        // Intercept: activating an inactive announcement
        boolean willActivate = request.getActive() != null && request.getActive()
                && !Boolean.TRUE.equals(announcement.getActive());
        if (willActivate) {
            String title = request.getTitle() != null ? request.getTitle() : announcement.getTitle();
            String content = request.getContent() != null ? request.getContent() : announcement.getContent();
            contentReviewHelper.reviewAndGate("ANNOUNCEMENT", id, title, content);
        }

        // Block edits on active announcements with pending reviews
        if (Boolean.TRUE.equals(announcement.getActive()) && !willActivate) {
            contentReviewHelper.checkPendingReview("ANNOUNCEMENT", id);
        }

        if (request.getTag() != null) announcement.setTag(request.getTag());
        if (request.getTagEn() != null) announcement.setTagEn(request.getTagEn());
        if (request.getType() != null) announcement.setType(request.getType());
        if (request.getTitle() != null) announcement.setTitle(request.getTitle());
        if (request.getTitleEn() != null) announcement.setTitleEn(request.getTitleEn());
        if (request.getContent() != null) announcement.setContent(htmlSanitizer.sanitize(request.getContent()));
        if (request.getContentEn() != null) announcement.setContentEn(htmlSanitizer.sanitize(request.getContentEn()));
        if (request.getSortOrder() != null) announcement.setSortOrder(request.getSortOrder());
        if (request.getActive() != null) announcement.setActive(request.getActive());
        if (request.getLevel() != null) announcement.setLevel(request.getLevel());
        if (request.getDisplayStyle() != null) announcement.setDisplayStyle(request.getDisplayStyle());
        announcement = announcementRepository.save(announcement);

        // Auto-translate if active and English fields are missing
        if (Boolean.TRUE.equals(announcement.getActive()) && needsTranslation(announcement)) {
            announcementTranslationService.translateAnnouncement(announcement.getId());
        }

        return toResponse(announcement);
    }

    @Transactional
    public void activateDirect(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANNOUNCEMENT_NOT_FOUND));
        announcement.setActive(true);
        announcementRepository.save(announcement);
    }

    @Transactional
    public void delete(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANNOUNCEMENT_NOT_FOUND));
        contentReviewHelper.deleteReviewsForContent("ANNOUNCEMENT", id);
        announcementRepository.delete(announcement);
    }

    @Transactional
    public void toggleActive(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANNOUNCEMENT_NOT_FOUND));
        announcement.setActive(!Boolean.TRUE.equals(announcement.getActive()));
        announcementRepository.save(announcement);
    }

    @Transactional
    public void bulkDelete(List<Long> ids) {
        for (Long id : ids) {
            contentReviewHelper.deleteReviewsForContent("ANNOUNCEMENT", id);
        }
        announcementRepository.deleteAllById(ids);
    }

    private AnnouncementResponse toResponse(Announcement a) {
        AnnouncementResponse r = new AnnouncementResponse();
        r.setId(a.getId());
        r.setTag(a.getTag());
        r.setTagEn(a.getTagEn());
        r.setType(a.getType());
        r.setTitle(a.getTitle());
        r.setTitleEn(a.getTitleEn());
        r.setContent(a.getContent());
        r.setContentEn(a.getContentEn());
        r.setSortOrder(a.getSortOrder());
        r.setActive(a.getActive());
        r.setLevel(a.getLevel());
        r.setDisplayStyle(a.getDisplayStyle());
        r.setCreatedAt(a.getCreatedAt());
        r.setUpdatedAt(a.getUpdatedAt());
        return r;
    }

    private PageRequest buildPageable(int page, int size, String sort) {
        Sort s = switch (sort != null ? sort : "") {
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            default -> Sort.by(Sort.Direction.ASC, "sortOrder").and(Sort.by(Sort.Direction.DESC, "createdAt"));
        };
        return PageRequest.of(page, size, s);
    }

    private void checkSensitiveContent(String tag, String title, String content) {
        // 管理员和超级管理员跳过内容检测
        String role = currentUserProvider.getCurrentUser().getRole();
        if ("admin".equals(role) || "super_admin".equals(role)) return;

        String result;
        if (tag != null) {
            result = sensitiveWordFilter.check(tag);
            if (result != null) throw new BusinessException(ErrorCode.SENSITIVE_WORD_BLOCKED.getCode(), "标签" + result);
        }
        if (title != null) {
            result = sensitiveWordFilter.check(title);
            if (result != null) throw new BusinessException(ErrorCode.SENSITIVE_WORD_BLOCKED.getCode(), "标题" + result);
        }
        if (content != null) {
            result = sensitiveWordFilter.checkContent(content);
            if (result != null) throw new BusinessException(ErrorCode.SENSITIVE_WORD_BLOCKED.getCode(), "内容" + result);
        }
    }

    private boolean needsTranslation(Announcement a) {
        return isBlank(a.getTitleEn()) || isBlank(a.getTagEn()) || isBlank(a.getContentEn());
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
