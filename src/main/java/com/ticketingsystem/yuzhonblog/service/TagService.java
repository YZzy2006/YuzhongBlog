package com.ticketingsystem.yuzhonblog.service;

import com.ticketingsystem.yuzhonblog.common.BusinessException;
import com.ticketingsystem.yuzhonblog.common.ErrorCode;
import com.ticketingsystem.yuzhonblog.dto.tag.TagRequest;
import com.ticketingsystem.yuzhonblog.dto.tag.TagResponse;
import com.ticketingsystem.yuzhonblog.entity.Tag;
import com.ticketingsystem.yuzhonblog.repository.ArticleTagRepository;
import com.ticketingsystem.yuzhonblog.repository.TagRepository;
import com.ticketingsystem.yuzhonblog.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final ArticleTagRepository articleTagRepository;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final CurrentUserProvider currentUserProvider;

    @Transactional(readOnly = true)
    public List<TagResponse> list() {
        List<Tag> tags = tagRepository.findAllByOrderByNameAsc();
        List<Long> ids = tags.stream().map(Tag::getId).collect(Collectors.toList());
        Map<Long, Long> countMap = ids.isEmpty() ? Map.of() :
            articleTagRepository.countPublishedByTagIds(ids).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        return tags.stream()
                .map(t -> toResponse(t, countMap.getOrDefault(t.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TagResponse getById(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TAG_NOT_FOUND));
        return toResponse(tag);
    }

    @Transactional
    public TagResponse create(TagRequest request) {
        checkSensitiveName(request.getName());
        if (tagRepository.existsByName(request.getName())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "标签名称已存在");
        }
        Tag tag = new Tag();
        tag.setName(request.getName());
        return toResponse(tagRepository.save(tag));
    }

    @Transactional
    public TagResponse update(Long id, TagRequest request) {
        checkSensitiveName(request.getName());
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TAG_NOT_FOUND));
        if (!tag.getName().equals(request.getName()) && tagRepository.existsByName(request.getName())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "标签名称已存在");
        }
        tag.setName(request.getName());
        return toResponse(tagRepository.save(tag));
    }

    @Transactional
    public void delete(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TAG_NOT_FOUND));
        if (articleTagRepository.countByTagId(id) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "该标签下还有文章，无法删除");
        }
        tagRepository.delete(tag);
    }

    private void checkSensitiveName(String name) {
        // 管理员和超级管理员跳过内容检测
        String role = currentUserProvider.getCurrentUser().getRole();
        if ("admin".equals(role) || "super_admin".equals(role)) return;

        if (name != null) {
            String result = sensitiveWordFilter.check(name);
            if (result != null) throw new BusinessException(ErrorCode.SENSITIVE_WORD_BLOCKED.getCode(), "标签名称" + result);
        }
    }

    private TagResponse toResponse(Tag tag) {
        return toResponse(tag, articleTagRepository.countPublishedByTagId(tag.getId()));
    }

    private TagResponse toResponse(Tag tag, long articleCount) {
        TagResponse resp = new TagResponse();
        resp.setId(tag.getId());
        resp.setName(tag.getName());
        resp.setCreatedAt(tag.getCreatedAt());
        resp.setArticleCount(articleCount);
        return resp;
    }
}
