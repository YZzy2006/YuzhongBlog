package com.ticketingsystem.yuzhonblog.controller;

import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.dto.PageResult;
import com.ticketingsystem.yuzhonblog.dto.article.ArticleDetailResponse;
import com.ticketingsystem.yuzhonblog.dto.article.ArticleListItem;
import com.ticketingsystem.yuzhonblog.security.IpExtractor;
import com.ticketingsystem.yuzhonblog.service.ArticleService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;
    private final IpExtractor ipExtractor;

    @GetMapping
    public ApiResponse<PageResult<ArticleListItem>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "latest") String sort) {
        size = Math.min(size, 100);
        return ApiResponse.success(articleService.getPublishedList(page, size, categoryId, tagId, keyword, startDate, endDate, sort));
    }

    @GetMapping("/{slug}")
    public ApiResponse<ArticleDetailResponse> detail(@PathVariable String slug, HttpServletRequest request) {
        String ip = ipExtractor.extractClientIp(request);
        return ApiResponse.success(articleService.getDetailBySlug(slug, ip));
    }

    @PostMapping("/{slug}/like")
    public ApiResponse<Map<String, Object>> like(@PathVariable String slug, HttpServletRequest request) {
        String ip = ipExtractor.extractClientIp(request);
        return ApiResponse.success(articleService.like(slug, ip));
    }
}
