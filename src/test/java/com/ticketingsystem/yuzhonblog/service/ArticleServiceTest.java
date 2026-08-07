package com.ticketingsystem.yuzhonblog.service;

import com.ticketingsystem.yuzhonblog.common.BusinessException;
import com.ticketingsystem.yuzhonblog.dto.PageResult;
import com.ticketingsystem.yuzhonblog.dto.article.ArticleCreateRequest;
import com.ticketingsystem.yuzhonblog.dto.article.ArticleListItem;
import com.ticketingsystem.yuzhonblog.dto.article.ArticleResponse;
import com.ticketingsystem.yuzhonblog.entity.Article;
import com.ticketingsystem.yuzhonblog.entity.Category;
import com.ticketingsystem.yuzhonblog.repository.ArticleRepository;
import com.ticketingsystem.yuzhonblog.repository.ArticleTagRepository;
import com.ticketingsystem.yuzhonblog.repository.CategoryRepository;
import com.ticketingsystem.yuzhonblog.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    private ArticleRepository articleRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private ArticleTagRepository articleTagRepository;
    @Mock
    private OssService ossService;
    @Mock
    private SensitiveWordFilter sensitiveWordFilter;
    @Mock
    private ContentReviewHelper contentReviewHelper;

    @InjectMocks
    private ArticleService articleService;

    // --- getPublishedList ---

    @Test
    void getPublishedList_NoCategoryId_ReturnsPublishedArticles() {
        // given
        Article article = buildArticle(1L, "Test Title", "test-slug", 1);
        Page<Article> page = new PageImpl<>(List.of(article), PageRequest.of(0, 10), 1);
        when(articleRepository.findPublishedWithCategory(eq(1), any(PageRequest.class)))
                .thenReturn(page);
        when(articleTagRepository.findByArticleIdInWithTags(anyList())).thenReturn(Collections.emptyList());

        // when
        PageResult<ArticleListItem> result = articleService.getPublishedList(0, 10, null, null, null, null, null, null);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Test Title");
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(articleRepository).findPublishedWithCategory(eq(1), any(PageRequest.class));
    }

    @Test
    void getPublishedList_WithCategoryId_FiltersByCategory() {
        // given
        Article article = buildArticle(1L, "Filtered", "filtered-slug", 1);
        Page<Article> page = new PageImpl<>(List.of(article), PageRequest.of(0, 10), 1);
        when(articleRepository.findPublishedWithCategoryByCategoryId(eq(1), eq(5L), any(PageRequest.class)))
                .thenReturn(page);
        when(articleTagRepository.findByArticleIdInWithTags(anyList())).thenReturn(Collections.emptyList());

        // when
        PageResult<ArticleListItem> result = articleService.getPublishedList(0, 10, 5L, null, null, null, null, null);

        // then
        assertThat(result.getContent()).hasSize(1);
        verify(articleRepository).findPublishedWithCategoryByCategoryId(eq(1), eq(5L), any(PageRequest.class));
    }

    @Test
    void getPublishedList_WithKeyword_SearchesByKeyword() {
        // given
        Article article = buildArticle(1L, "Search Result", "search-result", 1);
        Page<Article> page = new PageImpl<>(List.of(article), PageRequest.of(0, 10), 1);
        when(articleRepository.searchPublishedByKeyword(eq(1), eq("test"), any(PageRequest.class)))
                .thenReturn(page);
        when(articleTagRepository.findByArticleIdInWithTags(anyList())).thenReturn(Collections.emptyList());

        // when
        PageResult<ArticleListItem> result = articleService.getPublishedList(0, 10, null, null, "test", null, null, null);

        // then
        assertThat(result.getContent()).hasSize(1);
        verify(articleRepository).searchPublishedByKeyword(eq(1), eq("test"), any(PageRequest.class));
    }

    // --- getBySlug ---

    @Test
    void getBySlug_ExistingSlug_ReturnsResponseAndIncrementsViewCount() {
        // given
        Article article = buildArticle(1L, "My Post", "my-post", 1);
        article.setViewCount(10);
        when(articleRepository.findBySlug("my-post")).thenReturn(Optional.of(article));
        when(articleRepository.incrementViewCount(1L)).thenReturn(1);
        when(articleTagRepository.findTagIdsByArticleId(1L)).thenReturn(Collections.emptyList());

        // when
        ArticleResponse response = articleService.getBySlug("my-post");

        // then
        assertThat(response.getTitle()).isEqualTo("My Post");
        assertThat(response.getSlug()).isEqualTo("my-post");
        verify(articleRepository).incrementViewCount(1L);
    }

    @Test
    void getBySlug_NonExistentSlug_ThrowsBusinessException() {
        // given
        when(articleRepository.findBySlug("no-such-slug")).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> articleService.getBySlug("no-such-slug"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(2001));
    }

    // --- create ---

    @Test
    void create_ValidRequest_CreatesArticleAsDraft() {
        // given
        ArticleCreateRequest request = new ArticleCreateRequest();
        request.setTitle("New Article");
        request.setContentMd("# Hello");
        request.setContentHtml("<h1>Hello</h1>");
        request.setSummary("A summary");

        when(articleRepository.save(any(Article.class))).thenAnswer(inv -> {
            Article a = inv.getArgument(0);
            a.setId(100L);
            return a;
        });
        when(articleTagRepository.findTagIdsByArticleId(anyLong())).thenReturn(Collections.emptyList());

        // when
        ArticleResponse response = articleService.create(request);

        // then
        assertThat(response.getTitle()).isEqualTo("New Article");
        verify(articleRepository).save(argThat(a -> a.getStatus() == 0)); // draft
    }

    @Test
    void create_DuplicateSlug_ThrowsBusinessException() {
        // given
        ArticleCreateRequest request = new ArticleCreateRequest();
        request.setTitle("Dup");
        request.setSlug("existing-slug");
        when(articleRepository.existsBySlug("existing-slug")).thenReturn(true);

        // when / then
        assertThatThrownBy(() -> articleService.create(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(2002));
    }

    // --- delete ---

    @Test
    void delete_NonExistentId_ThrowsBusinessException() {
        // given
        when(articleRepository.findById(999L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> articleService.delete(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(2001));
    }

    @Test
    void delete_ExistingId_DeletesSuccessfully() {
        // given
        Article article = new Article();
        article.setId(1L);
        when(articleRepository.findById(1L)).thenReturn(Optional.of(article));

        // when
        articleService.delete(1L);

        // then
        verify(articleTagRepository).deleteByArticleId(1L);
        verify(articleRepository).deleteById(1L);
    }

    // --- helpers ---

    private Article buildArticle(Long id, String title, String slug, Integer status) {
        Article article = new Article();
        article.setId(id);
        article.setTitle(title);
        article.setSlug(slug);
        article.setStatus(status);
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setIsTop(0);
        return article;
    }
}
