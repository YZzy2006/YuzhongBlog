package com.ticketingsystem.yuzhonblog.controller.admin;

import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.dto.PageResult;
import com.ticketingsystem.yuzhonblog.dto.project.ProjectRequest;
import com.ticketingsystem.yuzhonblog.dto.project.ProjectResponse;
import com.ticketingsystem.yuzhonblog.security.RequirePermission;
import com.ticketingsystem.yuzhonblog.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/projects")
@RequiredArgsConstructor
public class ProjectAdminController {

    private final ProjectService projectService;

    @GetMapping
    @RequirePermission("project:view")
    public ApiResponse<PageResult<ProjectResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "default") String sort) {
        size = Math.min(size, 100);
        return ApiResponse.success(projectService.getAdminList(page, size, keyword, status, sort));
    }

    @GetMapping("/{id}")
    @RequirePermission("project:view")
    public ApiResponse<ProjectResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(projectService.getById(id));
    }

    @PostMapping
    @RequirePermission("project:manage")
    public ApiResponse<ProjectResponse> create(@Valid @RequestBody ProjectRequest request) {
        return ApiResponse.success(projectService.create(request));
    }

    @PutMapping("/{id}")
    @RequirePermission("project:manage")
    public ApiResponse<ProjectResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody ProjectRequest request) {
        return ApiResponse.success(projectService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @RequirePermission("project:manage")
    public ApiResponse<ProjectResponse> toggleStatus(@PathVariable Long id) {
        return ApiResponse.success(projectService.toggleStatus(id));
    }

    @PatchMapping("/{id}/featured")
    @RequirePermission("project:manage")
    public ApiResponse<ProjectResponse> toggleFeatured(@PathVariable Long id) {
        return ApiResponse.success(projectService.toggleFeatured(id));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("project:manage")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return ApiResponse.success();
    }

    @DeleteMapping
    @RequirePermission("project:manage")
    public ApiResponse<Void> bulkDelete(@RequestParam List<Long> ids) {
        projectService.bulkDelete(ids);
        return ApiResponse.success();
    }
}
