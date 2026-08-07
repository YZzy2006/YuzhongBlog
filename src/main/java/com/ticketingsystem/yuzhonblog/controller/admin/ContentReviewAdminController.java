package com.ticketingsystem.yuzhonblog.controller.admin;

import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.dto.PageResult;
import com.ticketingsystem.yuzhonblog.dto.review.ContentReviewResponse;
import com.ticketingsystem.yuzhonblog.dto.review.ReviewActionRequest;
import com.ticketingsystem.yuzhonblog.entity.ContentReview;
import com.ticketingsystem.yuzhonblog.repository.ContentReviewRepository;
import com.ticketingsystem.yuzhonblog.security.RequirePermission;
import com.ticketingsystem.yuzhonblog.service.ContentReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/reviews")
@RequiredArgsConstructor
public class ContentReviewAdminController {

    private final ContentReviewRepository contentReviewRepository;
    private final ContentReviewService contentReviewService;

    @GetMapping
    @RequirePermission("review:view")
    public ApiResponse<PageResult<ContentReviewResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        int safeSize = Math.min(size, 50);
        Pageable pageable = PageRequest.of(page, safeSize);
        Page<ContentReview> reviews = (status != null && !status.isBlank())
                ? contentReviewRepository.findByReviewStatusOrderByCreatedAtDesc(status, pageable)
                : contentReviewRepository.findAllByOrderByCreatedAtDesc(pageable);
        return ApiResponse.success(PageResult.of(reviews.map(this::toResponse)));
    }

    @PostMapping("/{id}/approve")
    @RequirePermission("review:manage")
    public ApiResponse<Void> approve(@PathVariable Long id,
                                      @RequestBody(required = false) ReviewActionRequest request) {
        contentReviewService.approve(id, request != null ? request.getComment() : null);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/reject")
    @RequirePermission("review:manage")
    public ApiResponse<Void> reject(@PathVariable Long id,
                                     @RequestBody(required = false) ReviewActionRequest request) {
        contentReviewService.reject(id, request != null ? request.getComment() : null);
        return ApiResponse.success();
    }

    private ContentReviewResponse toResponse(ContentReview r) {
        ContentReviewResponse resp = new ContentReviewResponse();
        resp.setId(r.getId());
        resp.setContentType(r.getContentType());
        resp.setContentId(r.getContentId());
        resp.setContentTitle(r.getContentTitle());
        resp.setSubmittedByName(r.getSubmittedByName());
        resp.setReviewStatus(r.getReviewStatus());
        resp.setAiAnalysis(r.getAiAnalysis());
        resp.setReviewedBy(r.getReviewedBy());
        resp.setReviewedAt(r.getReviewedAt());
        resp.setReviewComment(r.getReviewComment());
        resp.setCreatedAt(r.getCreatedAt());
        return resp;
    }
}
