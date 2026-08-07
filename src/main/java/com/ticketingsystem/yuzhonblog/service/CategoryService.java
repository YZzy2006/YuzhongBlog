package com.ticketingsystem.yuzhonblog.service;

import com.ticketingsystem.yuzhonblog.common.BusinessException;
import com.ticketingsystem.yuzhonblog.common.ErrorCode;
import com.ticketingsystem.yuzhonblog.dto.category.CategoryRequest;
import com.ticketingsystem.yuzhonblog.dto.category.CategoryResponse;
import com.ticketingsystem.yuzhonblog.entity.Category;
import com.ticketingsystem.yuzhonblog.repository.ArticleRepository;
import com.ticketingsystem.yuzhonblog.repository.CategoryRepository;
import com.ticketingsystem.yuzhonblog.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ArticleRepository articleRepository;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final CurrentUserProvider currentUserProvider;

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        List<Category> categories = categoryRepository.findAllByOrderBySortOrderDesc();
        List<Long> ids = categories.stream().map(Category::getId).collect(Collectors.toList());
        Map<Long, Long> countMap = ids.isEmpty() ? Map.of() :
            articleRepository.countPublishedByCategoryIds(ids).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        return categories.stream()
                .map(c -> toResponse(c, countMap.getOrDefault(c.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        return toResponse(category);
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        checkSensitiveName(request.getName());
        if (categoryRepository.existsByName(request.getName())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "分类名称已存在");
        }
        Category category = new Category();
        category.setName(request.getName());
        category.setSortOrder(request.getSortOrder());
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        checkSensitiveName(request.getName());
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        if (!category.getName().equals(request.getName()) && categoryRepository.existsByName(request.getName())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "分类名称已存在");
        }
        category.setName(request.getName());
        category.setSortOrder(request.getSortOrder());
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        if (articleRepository.countByCategoryId(id) > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_HAS_ARTICLES);
        }
        categoryRepository.delete(category);
    }

    private void checkSensitiveName(String name) {
        // 管理员和超级管理员跳过内容检测
        String role = currentUserProvider.getCurrentUser().getRole();
        if ("admin".equals(role) || "super_admin".equals(role)) return;

        if (name != null) {
            String result = sensitiveWordFilter.check(name);
            if (result != null) throw new BusinessException(ErrorCode.SENSITIVE_WORD_BLOCKED.getCode(), "分类名称" + result);
        }
    }

    private CategoryResponse toResponse(Category category) {
        return toResponse(category, articleRepository.countPublishedByCategoryId(category.getId()));
    }

    private CategoryResponse toResponse(Category category, long articleCount) {
        CategoryResponse resp = new CategoryResponse();
        resp.setId(category.getId());
        resp.setName(category.getName());
        resp.setSortOrder(category.getSortOrder());
        resp.setCreatedAt(category.getCreatedAt());
        resp.setArticleCount(articleCount);
        return resp;
    }
}
