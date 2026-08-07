package com.ticketingsystem.yuzhonblog.controller;

import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.dto.project.ProjectResponse;
import com.ticketingsystem.yuzhonblog.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ApiResponse<List<ProjectResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String techStack,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo) {
        return ApiResponse.success(projectService.searchPublic(keyword, techStack, featured, dateFrom, dateTo));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(projectService.getPublishedById(id));
    }
}