package com.ticketingsystem.yuzhonblog.controller.admin;

import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.dto.PageResult;
import com.ticketingsystem.yuzhonblog.dto.article.*;
import com.ticketingsystem.yuzhonblog.security.RequirePermission;
import com.ticketingsystem.yuzhonblog.service.ArticleService;
import com.ticketingsystem.yuzhonblog.service.ArticleTranslationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/articles")
@RequiredArgsConstructor
public class ArticleAdminController {

    private final ArticleService articleService;
    private final ArticleTranslationService articleTranslationService;

    @GetMapping
    @RequirePermission("article:view")
    public ApiResponse<PageResult<ArticleListItem>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId) {
        size = Math.min(size, 100);
        return ApiResponse.success(articleService.getAdminList(page, size, keyword, status, categoryId, tagId));
    }

    @GetMapping("/{id}")
    @RequirePermission("article:view")
    public ApiResponse<ArticleResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(articleService.getById(id));
    }

    @PostMapping
    @RequirePermission("article:create")
    public ApiResponse<ArticleResponse> create(@Valid @RequestBody ArticleCreateRequest request) {
        return ApiResponse.success(articleService.create(request));
    }

    @PutMapping("/{id}")
    @RequirePermission("article:edit")
    public ApiResponse<ArticleResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody ArticleUpdateRequest request) {
        return ApiResponse.success(articleService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @RequirePermission("article:publish")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                          @RequestParam Integer status) {
        articleService.updateStatus(id, status);
        return ApiResponse.success();
    }

    @PatchMapping("/{id}/top")
    @RequirePermission("article:edit")
    public ApiResponse<Void> updateTop(@PathVariable Long id,
                                       @RequestParam Integer isTop) {
        articleService.updateTop(id, isTop);
        return ApiResponse.success();
    }

    @PatchMapping("/{id}/featured")
    @RequirePermission("article:edit")
    public ApiResponse<Void> updateFeatured(@PathVariable Long id,
                                            @RequestParam Integer isFeatured) {
        articleService.updateFeatured(id, isFeatured);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("article:delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        articleService.delete(id);
        return ApiResponse.success();
    }

    @PostMapping("/translate")
    @RequirePermission("article:edit")
    public ApiResponse<String> translateAll() {
        int count = articleTranslationService.translateAllPending();
        return ApiResponse.success("Queued " + count + " articles for translation");
    }
}
