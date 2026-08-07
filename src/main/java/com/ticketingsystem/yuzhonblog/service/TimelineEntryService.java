package com.ticketingsystem.yuzhonblog.service;

import com.ticketingsystem.yuzhonblog.common.BusinessException;
import com.ticketingsystem.yuzhonblog.common.ErrorCode;
import com.ticketingsystem.yuzhonblog.dto.PageResult;
import com.ticketingsystem.yuzhonblog.dto.timeline.TimelineEntryRequest;
import com.ticketingsystem.yuzhonblog.dto.timeline.TimelineEntryResponse;
import com.ticketingsystem.yuzhonblog.entity.TimelineEntry;
import com.ticketingsystem.yuzhonblog.entity.TimelineLike;
import com.ticketingsystem.yuzhonblog.repository.TimelineEntryRepository;
import com.ticketingsystem.yuzhonblog.repository.TimelineLikeRepository;
import com.ticketingsystem.yuzhonblog.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimelineEntryService {

    private final TimelineEntryRepository timelineEntryRepository;
    private final TimelineLikeRepository timelineLikeRepository;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final ContentReviewHelper contentReviewHelper;
    private final CurrentUserProvider currentUserProvider;

    @Transactional(readOnly = true)
    public List<TimelineEntryResponse> list() {
        return timelineEntryRepository.findByStatusOrderByEntryDateDescSortOrderDesc("PUBLISHED").stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TimelineEntryResponse> listAll() {
        return timelineEntryRepository.findAllByOrderByEntryDateDescSortOrderDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResult<TimelineEntryResponse> getAdminList(int page, int size, String keyword, String status, String sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Sort s = "oldest".equals(sort) ? Sort.by(Sort.Direction.ASC, "entryDate")
                : Sort.by(Sort.Direction.DESC, "entryDate");
        PageRequest pageable = PageRequest.of(safePage, safeSize, s);
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        String st = (status != null && !status.isBlank()) ? status.trim() : null;
        Page<TimelineEntry> result = timelineEntryRepository.findAdminFiltered(kw, st, pageable);
        return PageResult.of(result.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public TimelineEntryResponse getById(Long id) {
        TimelineEntry entry = timelineEntryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TIMELINE_ENTRY_NOT_FOUND));
        return toResponse(entry);
    }

    @Transactional(readOnly = true)
    public TimelineEntryResponse getPublishedById(Long id) {
        TimelineEntry entry = timelineEntryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TIMELINE_ENTRY_NOT_FOUND));
        if (!"PUBLISHED".equals(entry.getStatus())) {
            throw new BusinessException(ErrorCode.TIMELINE_ENTRY_NOT_FOUND);
        }
        return toResponse(entry);
    }

    @Transactional
    public TimelineEntryResponse getDetailById(Long id) {
        TimelineEntry entry = timelineEntryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TIMELINE_ENTRY_NOT_FOUND));
        if (!"PUBLISHED".equals(entry.getStatus())) {
            throw new BusinessException(ErrorCode.TIMELINE_ENTRY_NOT_FOUND);
        }
        timelineEntryRepository.incrementViewCount(id);
        entry.setViewCount((entry.getViewCount() == null ? 0 : entry.getViewCount()) + 1);
        return toResponse(entry);
    }

    @Transactional
    public Map<String, Object> like(Long id, String ipAddress) {
        if (!timelineEntryRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.TIMELINE_ENTRY_NOT_FOUND);
        }
        Optional<TimelineLike> existing = timelineLikeRepository.findByEntryIdAndIpAddress(id, ipAddress);
        if (existing.isPresent()) {
            timelineLikeRepository.delete(existing.get());
            timelineEntryRepository.decrementLikeCount(id);
        } else {
            TimelineEntry entryRef = timelineEntryRepository.getReferenceById(id);
            TimelineLike like = new TimelineLike();
            like.setEntry(entryRef);
            like.setIpAddress(ipAddress);
            timelineLikeRepository.save(like);
            timelineEntryRepository.incrementLikeCount(id);
        }
        // Flush to ensure DB is updated, then read the actual count
        timelineEntryRepository.flush();
        int actualCount = timelineEntryRepository.findLikeCountById(id).orElse(0);
        boolean nowLiked = existing.isEmpty();
        return Map.of("likeCount", actualCount, "liked", nowLiked);
    }

    @Transactional(readOnly = true)
    public boolean isLikedByIp(Long id, String ipAddress) {
        return timelineLikeRepository.existsByEntryIdAndIpAddress(id, ipAddress);
    }

    @Transactional
    public TimelineEntryResponse create(TimelineEntryRequest request) {
        checkSensitiveContent(request);
        TimelineEntry entry = new TimelineEntry();
        copyFields(entry, request);
        return toResponse(timelineEntryRepository.save(entry));
    }

    @Transactional
    public TimelineEntryResponse update(Long id, TimelineEntryRequest request) {
        checkSensitiveContent(request);
        contentReviewHelper.checkPendingReview("TIMELINE", id);
        TimelineEntry entry = timelineEntryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TIMELINE_ENTRY_NOT_FOUND));
        copyFields(entry, request);
        return toResponse(timelineEntryRepository.save(entry));
    }

    @Transactional
    public TimelineEntryResponse toggleStatus(Long id) {
        TimelineEntry entry = timelineEntryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TIMELINE_ENTRY_NOT_FOUND));

        boolean willPublish = "DRAFT".equals(entry.getStatus());

        if (willPublish) {
            contentReviewHelper.checkPendingReview("TIMELINE", id);
            String bodyForReview = entry.getTitle() + "\n\n" +
                    (entry.getDescription() != null ? entry.getDescription() : "");
            contentReviewHelper.reviewAndGate("TIMELINE", id, entry.getTitle(), bodyForReview);
        }

        entry.setStatus(willPublish ? "PUBLISHED" : "DRAFT");
        return toResponse(timelineEntryRepository.save(entry));
    }

    @Transactional
    public void delete(Long id) {
        if (!timelineEntryRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.TIMELINE_ENTRY_NOT_FOUND);
        }
        contentReviewHelper.deleteReviewsForContent("TIMELINE", id);
        timelineEntryRepository.deleteById(id);
    }

    private void copyFields(TimelineEntry entry, TimelineEntryRequest request) {
        entry.setTitle(request.getTitle());
        entry.setDescription(request.getDescription());
        entry.setCoverImage(request.getCoverImage());
        entry.setEntryDate(request.getEntryDate());
        entry.setLinkUrl(request.getLinkUrl());
        entry.setCategory(request.getCategory());
        entry.setSortOrder(request.getSortOrder());
        if (request.getStatus() != null) {
            entry.setStatus(request.getStatus());
        }
        entry.setMood(request.getMood());
        entry.setTags(request.getTags());
        entry.setImages(request.getImages());
    }

    private TimelineEntryResponse toResponse(TimelineEntry entry) {
        TimelineEntryResponse resp = new TimelineEntryResponse();
        resp.setId(entry.getId());
        resp.setTitle(entry.getTitle());
        resp.setDescription(entry.getDescription());
        resp.setCoverImage(entry.getCoverImage());
        resp.setEntryDate(entry.getEntryDate());
        resp.setLinkUrl(entry.getLinkUrl());
        resp.setCategory(entry.getCategory());
        resp.setSortOrder(entry.getSortOrder());
        resp.setStatus(entry.getStatus());
        resp.setMood(entry.getMood());
        resp.setTags(entry.getTags());
        resp.setImages(entry.getImages());
        resp.setCreatedAt(entry.getCreatedAt());
        resp.setUpdatedAt(entry.getUpdatedAt());
        resp.setViewCount(entry.getViewCount());
        resp.setLikeCount(entry.getLikeCount());
        return resp;
    }

    private void checkSensitiveContent(TimelineEntryRequest request) {
        String role = currentUserProvider.getCurrentUser().getRole();
        if ("admin".equals(role) || "super_admin".equals(role)) return;

        String result;
        if (request.getTitle() != null) {
            result = sensitiveWordFilter.check(request.getTitle());
            if (result != null) throw new BusinessException(ErrorCode.SENSITIVE_WORD_BLOCKED.getCode(), "标题" + result);
        }
        if (request.getDescription() != null) {
            result = sensitiveWordFilter.checkContent(request.getDescription());
            if (result != null) throw new BusinessException(ErrorCode.SENSITIVE_WORD_BLOCKED.getCode(), "描述" + result);
        }
        if (request.getCategory() != null) {
            result = sensitiveWordFilter.check(request.getCategory());
            if (result != null) throw new BusinessException(ErrorCode.SENSITIVE_WORD_BLOCKED.getCode(), "分类" + result);
        }
    }
}
