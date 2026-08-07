package com.ticketingsystem.yuzhonblog.controller.admin;

import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.dto.tag.TagRequest;
import com.ticketingsystem.yuzhonblog.dto.tag.TagResponse;
import com.ticketingsystem.yuzhonblog.security.RequirePermission;
import com.ticketingsystem.yuzhonblog.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/tags")
@RequiredArgsConstructor
public class TagAdminController {

    private final TagService tagService;

    @GetMapping
    @RequirePermission("tag:view")
    public ApiResponse<List<TagResponse>> list() {
        return ApiResponse.success(tagService.list());
    }

    @GetMapping("/{id}")
    @RequirePermission("tag:view")
    public ApiResponse<TagResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(tagService.getById(id));
    }

    @PostMapping
    @RequirePermission("tag:manage")
    public ApiResponse<TagResponse> create(@Valid @RequestBody TagRequest request) {
        return ApiResponse.success(tagService.create(request));
    }

    @PutMapping("/{id}")
    @RequirePermission("tag:manage")
    public ApiResponse<TagResponse> update(@PathVariable Long id,
                                           @Valid @RequestBody TagRequest request) {
        return ApiResponse.success(tagService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("tag:manage")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return ApiResponse.success();
    }
}
