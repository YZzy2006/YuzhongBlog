package com.ticketingsystem.yuzhonblog.service;

import com.ticketingsystem.yuzhonblog.common.BusinessException;
import com.ticketingsystem.yuzhonblog.common.ErrorCode;
import com.ticketingsystem.yuzhonblog.dto.PageResult;
import com.ticketingsystem.yuzhonblog.dto.article.*;
import com.ticketingsystem.yuzhonblog.entity.*;
import com.ticketingsystem.yuzhonblog.repository.*;
import com.ticketingsystem.yuzhonblog.security.CurrentUserProvider;
import com.ticketingsystem.yuzhonblog.util.HtmlSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ArticleTagRepository articleTagRepository;
    private final ArticleLikeRepository articleLikeRepository;
    private final OssService ossService;
    private final HtmlSanitizer htmlSanitizer;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final ContentReviewHelper contentReviewHelper;
    private final CurrentUserProvider currentUserProvider;
    private final ArticleTranslationService articleTranslationService;

    // === Public ===

    @Transactional(readOnly = true)
    public PageResult<ArticleListItem> getPublishedList(int page, int size, Long categoryId, Long tagId, String keyword, String startDate, String endDate, String sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageRequest = PageRequest.of(safePage, safeSize);
        LocalDateTime start = parseStartDate(startDate);
        LocalDateTime end = parseEndDate(endDate);
        boolean hasDateRange = start != null || end != null;
        if (start == null) start = LocalDateTime.of(2000, 1, 1, 0, 0);
        if (end == null) end = LocalDateTime.now().plusDays(1);

        Page<Article> articles;
        if (keyword != null && !keyword.isBlank()) {
            if (hasDateRange) {
                articles = articleRepository.searchPublishedByKeywordWithDateRange(1, keyword, start, end, pageRequest);
            } else {
                articles = multiLevelSearch(keyword, pageRequest);
            }
        } else if (tagId != null) {
            if (hasDateRange) {
                articles = articleRepository.findPublishedWithTagAndDateRange(1, tagId, start, end, pageRequest);
            } else {
                articles = articleRepository.findByStatusAndTagId(1, tagId, pageRequest);
            }
        } else if (categoryId != null) {
            if (hasDateRange) {
                articles = articleRepository.findPublishedWithCategoryAndDateRange(1, categoryId, start, end, pageRequest);
            } else {
                articles = articleRepository.findPublishedWithCategoryByCategoryId(1, categoryId, pageRequest);
            }
        } else if ("popular".equals(sort)) {
            if (hasDateRange) {
                articles = articleRepository.findPublishedPopularWithDateRange(1, start, end, pageRequest);
            } else {
                articles = articleRepository.findPublishedPopular(1, pageRequest);
            }
        } else if ("featured".equals(sort)) {
            if (hasDateRange) {
                articles = articleRepository.findPublishedFeaturedWithDateRange(1, start, end, pageRequest);
            } else {
                articles = articleRepository.findPublishedFeatured(1, pageRequest);
            }
        } else {
            if (hasDateRange) {
                articles = articleRepository.findPublishedWithDateRange(1, start, end, pageRequest);
            } else {
                articles = articleRepository.findPublishedWithCategory(1, pageRequest);
            }
        }
        Map<Long, List<ArticleResponse.TagInfo>> tagsMap = batchLoadTags(articles.getContent());
        return PageResult.of(articles.map(a -> toListItem(a, tagsMap.getOrDefault(a.getId(), Collections.emptyList()))));
    }

    private LocalDateTime parseStartDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDate.parse(dateStr).atStartOfDay();
        } catch (java.time.format.DateTimeParseException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "日期格式无效: " + dateStr);
        }
    }

    private LocalDateTime parseEndDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDate.parse(dateStr).plusDays(1).atStartOfDay();
        } catch (java.time.format.DateTimeParseException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "日期格式无效: " + dateStr);
        }
    }

    /**
     * Multi-level search: progressively broaden the search scope.
     * Level 1: exact phrase in title + summary
     * Level 2: exact phrase in title + summary + content
     * Level 3: split keywords, each word in title + summary + content
     */
    private Page<Article> multiLevelSearch(String keyword, PageRequest pageRequest) {
        // Level 1: exact phrase in title + summary
        Page<Article> result = articleRepository.searchPublishedByKeyword(1, keyword, pageRequest);
        if (result.getTotalElements() > 0) return result;

        // Level 2: exact phrase in title + summary + content
        result = articleRepository.searchPublishedByKeywordDeep(1, keyword, pageRequest);
        if (result.getTotalElements() > 0) return result;

        // Level 3: split into words, search each in title + summary + content
        String[] words = keyword.trim().split("\\s+");
        if (words.length > 1) {
            result = articleRepository.searchPublishedByWords(1, words, pageRequest);
        }
        return result;
    }

    @Transactional
    public ArticleResponse getBySlug(String slug) {
        Article article = articleRepository.findBySlug(slug)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND));
        articleRepository.incrementViewCount(article.getId());
        article.setViewCount(article.getViewCount() + 1);
        return toResponse(article);
    }

    @Transactional
    public ArticleDetailResponse getDetailBySlug(String slug, String ipAddress) {
        Article article = articleRepository.findBySlug(slug).orElse(null);
        if (article == null) {
            try {
                article = articleRepository.findById(Long.parseLong(slug))
                        .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND));
            } catch (NumberFormatException e) {
                throw new BusinessException(ErrorCode.ARTICLE_NOT_FOUND);
            }
        }
        articleRepository.incrementViewCount(article.getId());
        article.setViewCount(article.getViewCount() + 1);

        ArticleDetailResponse resp = toDetailResponse(article);
        resp.setLiked(articleLikeRepository.existsByArticleIdAndIpAddress(article.getId(), ipAddress));

        // Prev / Next
        Article prev = articleRepository.findPrevious(article.getCreatedAt());
        if (prev != null) {
            resp.setPrevArticle(new ArticleDetailResponse.NavItem(prev.getId(), prev.getTitle(), prev.getTitleEn(), prev.getSlug()));
        }
        Article next = articleRepository.findNext(article.getCreatedAt());
        if (next != null) {
            resp.setNextArticle(new ArticleDetailResponse.NavItem(next.getId(), next.getTitle(), next.getTitleEn(), next.getSlug()));
        }

        // Related articles
        List<Article> related;
        var relatedPage = PageRequest.of(0, 4);
        if (article.getCategory() != null) {
            related = articleRepository.findRelated(article.getCategory().getId(), article.getId(), relatedPage);
            if (related.isEmpty()) {
                related = articleRepository.findRecentExclude(article.getId(), relatedPage);
            }
        } else {
            related = articleRepository.findRecentExclude(article.getId(), relatedPage);
        }
        resp.setRelatedArticles(related.stream().map(a -> {
            ArticleDetailResponse.RelatedArticle ra = new ArticleDetailResponse.RelatedArticle();
            ra.setId(a.getId());
            ra.setTitle(a.getTitle());
            ra.setTitleEn(a.getTitleEn());
            ra.setSummary(a.getSummary());
            ra.setSummaryEn(a.getSummaryEn());
            ra.setSlug(a.getSlug());
            ra.setViewCount(a.getViewCount());
            ra.setLikeCount(a.getLikeCount());
            if (a.getCategory() != null) {
                ra.setCategoryName(a.getCategory().getName());
            }
            return ra;
        }).collect(Collectors.toList()));

        return resp;
    }

    // === Admin ===

    @Transactional(readOnly = true)
    public PageResult<ArticleListItem> getAdminList(int page, int size, String keyword, Integer status, Long categoryId, Long tagId) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageRequest = PageRequest.of(safePage, safeSize);
        Page<Article> articles;
        if (keyword != null && !keyword.isBlank()) {
            articles = status != null
                    ? articleRepository.searchByKeyword(status, keyword, pageRequest)
                    : articleRepository.searchByKeywordAllStatuses(keyword, pageRequest);
        } else if (tagId != null) {
            articles = status != null
                    ? articleRepository.findByStatusAndTagId(status, tagId, pageRequest)
                    : articleRepository.findByTagId(tagId, pageRequest);
        } else if (categoryId != null) {
            articles = status != null
                    ? articleRepository.findByStatusAndCategoryIdOrderByIsTopDescCreatedAtDesc(status, categoryId, pageRequest)
                    : articleRepository.findByCategoryIdOrderByIsTopDescCreatedAtDesc(categoryId, pageRequest);
        } else if (status != null) {
            articles = articleRepository.findByStatusOrderByIsTopDescCreatedAtDesc(status, pageRequest);
        } else {
            articles = articleRepository.findAllWithCategory(pageRequest);
        }
        Map<Long, List<ArticleResponse.TagInfo>> tagsMap = batchLoadTags(articles.getContent());
        return PageResult.of(articles.map(a -> toListItem(a, tagsMap.getOrDefault(a.getId(), Collections.emptyList()))));
    }

    @Transactional(readOnly = true)
    public ArticleResponse getById(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND));
        return toResponse(article);
    }

    @Transactional
    public ArticleResponse create(ArticleCreateRequest request) {
        checkSensitiveContent(request.getTitle(), request.getSummary(), request.getContentMd());

        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            if (articleRepository.existsBySlug(request.getSlug())) {
                throw new BusinessException(ErrorCode.ARTICLE_SLUG_DUPLICATE);
            }
        }

        Article article = new Article();
        article.setTitle(request.getTitle());
        article.setTitleEn(request.getTitleEn());
        article.setContentMd(request.getContentMd());
        article.setContentMdEn(request.getContentMdEn());
        article.setContentHtml(htmlSanitizer.sanitize(request.getContentHtml()));
        article.setContentHtmlEn(htmlSanitizer.sanitize(request.getContentHtmlEn()));
        article.setSummary(request.getSummary());
        article.setSummaryEn(request.getSummaryEn());
        article.setCoverImage(request.getCoverImage());
        article.setSlug(request.getSlug());
        article.setStatus(0); // draft
        article.setCardStyle(request.getCardStyle() != null ? request.getCardStyle() : 0);
        article.setAuthorNotes(request.getAuthorNotes());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
            article.setCategory(category);
        }

        article = articleRepository.save(article);

        // Save tags
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            saveArticleTags(article, request.getTagIds());
        }

        return toResponse(article);
    }

    @Transactional
    public ArticleResponse update(Long id, ArticleUpdateRequest request) {
        checkSensitiveContent(request.getTitle(), request.getSummary(), request.getContentMd());
        contentReviewHelper.checkPendingReview("ARTICLE", id);

        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND));

        if (request.getTitle() != null) article.setTitle(request.getTitle());
        if (request.getTitleEn() != null) article.setTitleEn(request.getTitleEn());
        if (request.getContentMd() != null) article.setContentMd(request.getContentMd());
        if (request.getContentMdEn() != null) article.setContentMdEn(request.getContentMdEn());
        if (request.getContentHtml() != null) article.setContentHtml(htmlSanitizer.sanitize(request.getContentHtml()));
        if (request.getContentHtmlEn() != null) article.setContentHtmlEn(htmlSanitizer.sanitize(request.getContentHtmlEn()));
        if (request.getSummary() != null) article.setSummary(request.getSummary());
        if (request.getSummaryEn() != null) article.setSummaryEn(request.getSummaryEn());
        if (request.getCoverImage() != null) article.setCoverImage(request.getCoverImage());
        if (request.getCardStyle() != null) article.setCardStyle(request.getCardStyle());
        if (request.getAuthorNotes() != null) article.setAuthorNotes(request.getAuthorNotes());

        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            if (!request.getSlug().equals(article.getSlug()) && articleRepository.existsBySlug(request.getSlug())) {
                throw new BusinessException(ErrorCode.ARTICLE_SLUG_DUPLICATE);
            }
            article.setSlug(request.getSlug());
        }

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
            article.setCategory(category);
        } else {
            article.setCategory(null);
        }

        article = articleRepository.save(article);

        // Replace tags
        if (request.getTagIds() != null) {
            articleTagRepository.deleteByArticleId(id);
            if (!request.getTagIds().isEmpty()) {
                saveArticleTags(article, request.getTagIds());
            }
        }

        // Auto-translate if published and English fields are missing
        if (article.getStatus() == 1 && needsTranslation(article)) {
            articleTranslationService.translateArticle(id);
        }

        return toResponse(article);
    }

    @Transactional
    public void updateStatus(Long id, Integer status) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND));

        // Intercept publish action
        if (status == 1 && article.getStatus() != 1) {
            AdminUser currentUser = currentUserProvider.getCurrentUser();
            // 普通管理员发布文章需要超级管理员审核 → 设为待审核状态(3)
            if ("admin".equals(currentUser.getRole())) {
                contentReviewHelper.createPendingReview("ARTICLE", id, article.getTitle(), currentUser);
                article.setStatus(3); // pending review
                articleRepository.save(article);
                return;
            }
            // 超级管理员直接发布，仍需 AI 内容检测
            String bodyForReview = article.getTitle() + "\n\n" +
                    (article.getSummary() != null ? article.getSummary() + "\n\n" : "") +
                    (article.getContentMd() != null ? article.getContentMd() : "");
            contentReviewHelper.reviewAndGate("ARTICLE", id, article.getTitle(), bodyForReview);
        }

        article.setStatus(status);
        articleRepository.save(article);

        // Auto-translate on publish
        if (status == 1) {
            articleTranslationService.translateArticle(id);
        }
    }

    @Transactional
    public void updateStatusDirect(Long id, Integer status) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND));
        article.setStatus(status);
        articleRepository.save(article);

        // Auto-translate on publish
        if (status == 1) {
            articleTranslationService.translateArticle(id);
        }
    }

    @Transactional
    public void updateTop(Long id, Integer isTop) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND));
        article.setIsTop(isTop);
        articleRepository.save(article);
    }

    @Transactional
    public void updateFeatured(Long id, Integer isFeatured) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND));
        article.setIsFeatured(isFeatured);
        articleRepository.save(article);
    }

    @Transactional
    public void delete(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND));

        // Clean up OSS files
        if (article.getCoverImage() != null && !article.getCoverImage().isBlank()) {
            try { ossService.deleteFile(article.getCoverImage()); } catch (Exception e) { log.warn("Failed to delete OSS file: {}", article.getCoverImage(), e); }
        }

        articleTagRepository.deleteByArticleId(id);
        contentReviewHelper.deleteReviewsForContent("ARTICLE", id);
        articleRepository.deleteById(id);
    }

    @Transactional
    public Map<String, Object> like(String slug, String ipAddress) {
        Long articleId = articleRepository.findIdBySlug(slug)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND));

        boolean nowLiked;
        Optional<ArticleLike> existing = articleLikeRepository.findByArticleIdAndIpAddress(articleId, ipAddress);
        if (existing.isPresent()) {
            articleLikeRepository.delete(existing.get());
            articleRepository.decrementLikeCount(articleId);
            nowLiked = false;
        } else {
            try {
                Article articleRef = articleRepository.getReferenceById(articleId);
                ArticleLike like = new ArticleLike();
                like.setArticle(articleRef);
                like.setIpAddress(ipAddress);
                articleLikeRepository.save(like);
                articleRepository.incrementLikeCount(articleId);
                nowLiked = true;
            } catch (DataIntegrityViolationException e) {
                // Concurrent insert race condition — already liked by another request
                nowLiked = true;
            }
        }
        articleRepository.flush();
        int actualCount = articleRepository.findLikeCountById(articleId).orElse(0);
        return Map.of("likeCount", actualCount, "liked", nowLiked);
    }

    @Transactional(readOnly = true)
    public boolean isLikedByIp(String slug, String ipAddress) {
        Article article = articleRepository.findBySlug(slug)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND));
        return articleLikeRepository.existsByArticleIdAndIpAddress(article.getId(), ipAddress);
    }

    // === Helper ===

    private void saveArticleTags(Article article, List<Long> tagIds) {
        List<Tag> tags = tagRepository.findByIdIn(tagIds);
        List<ArticleTag> articleTags = tags.stream().map(tag -> {
            ArticleTag at = new ArticleTag();
            at.setArticle(article);
            at.setTag(tag);
            return at;
        }).toList();
        articleTagRepository.saveAll(articleTags);
    }

    private ArticleListItem toListItem(Article article, List<ArticleResponse.TagInfo> tags) {
        ArticleListItem item = new ArticleListItem();
        item.setId(article.getId());
        item.setTitle(article.getTitle());
        item.setTitleEn(article.getTitleEn());
        item.setSummary(article.getSummary());
        item.setSummaryEn(article.getSummaryEn());
        item.setCoverImage(article.getCoverImage());
        item.setSlug(article.getSlug());
        item.setStatus(article.getStatus());
        item.setViewCount(article.getViewCount());
        item.setLikeCount(article.getLikeCount());
        item.setIsTop(article.getIsTop());
        item.setIsFeatured(article.getIsFeatured());
        item.setCardStyle(article.getCardStyle());
        item.setCreatedAt(article.getCreatedAt());
        if (article.getCategory() != null) {
            item.setCategoryId(article.getCategory().getId());
            item.setCategoryName(article.getCategory().getName());
        }
        item.setTags(tags);
        return item;
    }

    private Map<Long, List<ArticleResponse.TagInfo>> batchLoadTags(List<Article> articles) {
        List<Long> articleIds = articles.stream().map(Article::getId).collect(Collectors.toList());
        if (articleIds.isEmpty()) return Collections.emptyMap();

        List<ArticleTag> articleTags = articleTagRepository.findByArticleIdInWithTags(articleIds);
        return articleTags.stream().collect(Collectors.groupingBy(
                at -> at.getArticle().getId(),
                Collectors.mapping(at -> {
                    ArticleResponse.TagInfo info = new ArticleResponse.TagInfo();
                    info.setId(at.getTag().getId());
                    info.setName(at.getTag().getName());
                    return info;
                }, Collectors.toList())
        ));
    }

    private ArticleResponse toResponse(Article article) {
        ArticleResponse resp = new ArticleResponse();
        resp.setId(article.getId());
        resp.setTitle(article.getTitle());
        resp.setTitleEn(article.getTitleEn());
        resp.setContentMd(article.getContentMd());
        resp.setContentMdEn(article.getContentMdEn());
        resp.setContentHtml(article.getContentHtml());
        resp.setContentHtmlEn(article.getContentHtmlEn());
        resp.setSummary(article.getSummary());
        resp.setSummaryEn(article.getSummaryEn());
        resp.setCoverImage(article.getCoverImage());
        resp.setSlug(article.getSlug());
        resp.setStatus(article.getStatus());
        resp.setViewCount(article.getViewCount());
        resp.setLikeCount(article.getLikeCount());
        resp.setIsTop(article.getIsTop());
        resp.setIsFeatured(article.getIsFeatured());
        resp.setCardStyle(article.getCardStyle());
        resp.setAuthorNotes(article.getAuthorNotes());
        resp.setCreatedAt(article.getCreatedAt());
        resp.setUpdatedAt(article.getUpdatedAt());
        if (article.getCategory() != null) {
            resp.setCategoryId(article.getCategory().getId());
            resp.setCategoryName(article.getCategory().getName());
        }
        resp.setTags(getTagInfos(article.getId()));
        return resp;
    }

    private ArticleDetailResponse toDetailResponse(Article article) {
        ArticleDetailResponse resp = new ArticleDetailResponse();
        resp.setId(article.getId());
        resp.setTitle(article.getTitle());
        resp.setTitleEn(article.getTitleEn());
        resp.setContentMd(article.getContentMd());
        resp.setContentMdEn(article.getContentMdEn());
        resp.setContentHtml(article.getContentHtml());
        resp.setContentHtmlEn(article.getContentHtmlEn());
        resp.setSummary(article.getSummary());
        resp.setSummaryEn(article.getSummaryEn());
        resp.setCoverImage(article.getCoverImage());
        resp.setSlug(article.getSlug());
        resp.setStatus(article.getStatus());
        resp.setViewCount(article.getViewCount());
        resp.setLikeCount(article.getLikeCount());
        resp.setIsTop(article.getIsTop());
        resp.setIsFeatured(article.getIsFeatured());
        resp.setCardStyle(article.getCardStyle());
        resp.setAuthorNotes(article.getAuthorNotes());
        resp.setCreatedAt(article.getCreatedAt());
        resp.setUpdatedAt(article.getUpdatedAt());
        if (article.getCategory() != null) {
            resp.setCategoryId(article.getCategory().getId());
            resp.setCategoryName(article.getCategory().getName());
        }
        resp.setTags(getTagInfos(article.getId()));
        return resp;
    }

    private List<ArticleResponse.TagInfo> getTagInfos(Long articleId) {
        List<Long> tagIds = articleTagRepository.findTagIdsByArticleId(articleId);
        if (tagIds.isEmpty()) return Collections.emptyList();
        return tagRepository.findByIdIn(tagIds).stream()
                .map(tag -> {
                    ArticleResponse.TagInfo info = new ArticleResponse.TagInfo();
                    info.setId(tag.getId());
                    info.setName(tag.getName());
                    return info;
                })
                .collect(Collectors.toList());
    }

    private void checkSensitiveContent(String title, String summary, String content) {
        // 管理员和超级管理员跳过内容检测
        String role = currentUserProvider.getCurrentUser().getRole();
        if ("admin".equals(role) || "super_admin".equals(role)) return;

        String result;
        if (title != null) {
            result = sensitiveWordFilter.check(title);
            if (result != null) throw new BusinessException(ErrorCode.SENSITIVE_WORD_BLOCKED.getCode(), "标题" + result);
        }
        if (summary != null) {
            result = sensitiveWordFilter.check(summary);
            if (result != null) throw new BusinessException(ErrorCode.SENSITIVE_WORD_BLOCKED.getCode(), "摘要" + result);
        }
        if (content != null) {
            result = sensitiveWordFilter.checkContent(content);
            if (result != null) throw new BusinessException(ErrorCode.SENSITIVE_WORD_BLOCKED.getCode(), "正文" + result);
        }
    }

    private boolean needsTranslation(Article article) {
        return isBlank(article.getTitleEn()) || isBlank(article.getSummaryEn()) || isBlank(article.getContentMdEn());
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
