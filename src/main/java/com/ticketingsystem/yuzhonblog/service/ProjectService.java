package com.ticketingsystem.yuzhonblog.service;

import com.ticketingsystem.yuzhonblog.common.BusinessException;
import com.ticketingsystem.yuzhonblog.common.ErrorCode;
import com.ticketingsystem.yuzhonblog.dto.PageResult;
import com.ticketingsystem.yuzhonblog.dto.project.ProjectRequest;
import com.ticketingsystem.yuzhonblog.dto.project.ProjectResponse;
import com.ticketingsystem.yuzhonblog.entity.Project;
import com.ticketingsystem.yuzhonblog.repository.ProjectRepository;
import com.ticketingsystem.yuzhonblog.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final ContentReviewHelper contentReviewHelper;
    private final CurrentUserProvider currentUserProvider;

    @Transactional(readOnly = true)
    public List<ProjectResponse> list() {
        return projectRepository.findByStatusOrderBySortOrderDescCreatedAtDesc("PUBLISHED").stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> searchPublic(String keyword, String techStack, Boolean featured, LocalDateTime dateFrom, LocalDateTime dateTo) {
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        String ts = (techStack != null && !techStack.isBlank()) ? techStack.trim() : null;
        return projectRepository.searchPublic(kw, ts, featured, dateFrom, dateTo).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listAll() {
        return projectRepository.findAllByOrderBySortOrderDescCreatedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResult<ProjectResponse> getAdminList(int page, int size, String keyword, String status, String sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Sort s = "oldest".equals(sort) ? Sort.by(Sort.Direction.ASC, "createdAt")
                : Sort.by(Sort.Direction.DESC, "createdAt");
        PageRequest pageable = PageRequest.of(safePage, safeSize, s);
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        String st = (status != null && !status.isBlank()) ? status.trim() : null;
        Page<Project> result = projectRepository.findAdminFiltered(kw, st, pageable);
        return PageResult.of(result.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public ProjectResponse getById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getPublishedById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        if (!"PUBLISHED".equals(project.getStatus())) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        }
        return toResponse(project);
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        checkSensitiveContent(request);
        Project project = new Project();
        copyFields(project, request);
        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse update(Long id, ProjectRequest request) {
        checkSensitiveContent(request);
        contentReviewHelper.checkPendingReview("PROJECT", id);
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        copyFields(project, request);
        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse toggleStatus(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

        boolean willPublish = "DRAFT".equals(project.getStatus());

        if (willPublish) {
            contentReviewHelper.checkPendingReview("PROJECT", id);
            String bodyForReview = project.getName() + "\n\n" +
                    (project.getSubtitle() != null ? project.getSubtitle() + "\n\n" : "") +
                    (project.getDescription() != null ? project.getDescription() : "");
            contentReviewHelper.reviewAndGate("PROJECT", id, project.getName(), bodyForReview);
        }

        project.setStatus(willPublish ? "PUBLISHED" : "DRAFT");
        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public void publishDirect(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        project.setStatus("PUBLISHED");
        projectRepository.save(project);
    }

    @Transactional
    public ProjectResponse toggleFeatured(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        project.setIsFeatured(!Boolean.TRUE.equals(project.getIsFeatured()));
        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public void delete(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        }
        contentReviewHelper.deleteReviewsForContent("PROJECT", id);
        projectRepository.deleteById(id);
    }

    @Transactional
    public void bulkDelete(List<Long> ids) {
        for (Long id : ids) {
            contentReviewHelper.deleteReviewsForContent("PROJECT", id);
        }
        projectRepository.deleteAllById(ids);
    }

    private void copyFields(Project project, ProjectRequest request) {
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setTechStack(request.getTechStack());
        project.setCoverImage(request.getCoverImage());
        project.setGithubUrl(request.getGithubUrl());
        project.setDemoUrl(request.getDemoUrl());
        project.setSortOrder(request.getSortOrder());
        project.setSubtitle(request.getSubtitle());
        project.setFeatures(request.getFeatures());
        project.setSubdomainUrl(request.getSubdomainUrl());
        project.setScreenshots(request.getScreenshots());
        if (request.getStatus() != null) {
            project.setStatus(request.getStatus());
        }
        if (request.getIsFeatured() != null) {
            project.setIsFeatured(request.getIsFeatured());
        }
    }

    private ProjectResponse toResponse(Project project) {
        ProjectResponse resp = new ProjectResponse();
        resp.setId(project.getId());
        resp.setName(project.getName());
        resp.setDescription(project.getDescription());
        resp.setTechStack(project.getTechStack());
        resp.setCoverImage(project.getCoverImage());
        resp.setGithubUrl(project.getGithubUrl());
        resp.setDemoUrl(project.getDemoUrl());
        resp.setSortOrder(project.getSortOrder());
        resp.setSubtitle(project.getSubtitle());
        resp.setFeatures(project.getFeatures());
        resp.setSubdomainUrl(project.getSubdomainUrl());
        resp.setScreenshots(project.getScreenshots());
        resp.setStatus(project.getStatus());
        resp.setIsFeatured(project.getIsFeatured());
        resp.setCreatedAt(project.getCreatedAt());
        resp.setUpdatedAt(project.getUpdatedAt());
        return resp;
    }

    private void checkSensitiveContent(ProjectRequest request) {
        // 管理员和超级管理员跳过内容检测
        String role = currentUserProvider.getCurrentUser().getRole();
        if ("admin".equals(role) || "super_admin".equals(role)) return;

        String result;
        if (request.getName() != null) {
            result = sensitiveWordFilter.check(request.getName());
            if (result != null) throw new BusinessException(ErrorCode.SENSITIVE_WORD_BLOCKED.getCode(), "项目名称" + result);
        }
        if (request.getSubtitle() != null) {
            result = sensitiveWordFilter.check(request.getSubtitle());
            if (result != null) throw new BusinessException(ErrorCode.SENSITIVE_WORD_BLOCKED.getCode(), "副标题" + result);
        }
        if (request.getDescription() != null) {
            result = sensitiveWordFilter.checkContent(request.getDescription());
            if (result != null) throw new BusinessException(ErrorCode.SENSITIVE_WORD_BLOCKED.getCode(), "项目描述" + result);
        }
        if (request.getTechStack() != null) {
            result = sensitiveWordFilter.checkContent(request.getTechStack());
            if (result != null) throw new BusinessException(ErrorCode.SENSITIVE_WORD_BLOCKED.getCode(), "技术栈" + result);
        }
        if (request.getFeatures() != null) {
            result = sensitiveWordFilter.checkContent(request.getFeatures());
            if (result != null) throw new BusinessException(ErrorCode.SENSITIVE_WORD_BLOCKED.getCode(), "功能亮点" + result);
        }
    }
}
