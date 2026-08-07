package com.ticketingsystem.yuzhonblog.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketingsystem.yuzhonblog.entity.Article;
import com.ticketingsystem.yuzhonblog.repository.ArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class ArticleControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ArticleRepository articleRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        articleRepository.deleteAll();
    }

    @Test
    void list_ReturnsEmptyPage_WhenNoArticles() throws Exception {
        mockMvc.perform(get("/api/articles")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void list_ReturnsPublishedArticles_WhenArticlesExist() throws Exception {
        // given: insert a published article directly via repository
        Article article = new Article();
        article.setTitle("Integration Test Article");
        article.setSlug("integration-test");
        article.setSummary("Testing the list endpoint");
        article.setStatus(1); // published
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setIsTop(0);
        articleRepository.save(article);

        // when / then
        MvcResult result = mockMvc.perform(get("/api/articles")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].title").value("Integration Test Article"))
                .andExpect(jsonPath("$.data.content[0].slug").value("integration-test"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andReturn();

        // verify structure: content array contains ArticleListItem fields
        JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("content").path(0);
        assertThat(content.has("id")).isTrue();
        assertThat(content.has("title")).isTrue();
        assertThat(content.has("slug")).isTrue();
        assertThat(content.has("status")).isTrue();
        assertThat(content.has("viewCount")).isTrue();
    }

    @Test
    void list_DoesNotReturnDraftArticles() throws Exception {
        // given: insert a draft article (status=0)
        Article draft = new Article();
        draft.setTitle("Draft Article");
        draft.setSlug("draft-article");
        draft.setStatus(0); // draft
        draft.setViewCount(0);
        draft.setLikeCount(0);
        draft.setIsTop(0);
        articleRepository.save(draft);

        // when / then: published list should not include drafts
        mockMvc.perform(get("/api/articles")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void detail_ReturnsArticle_WhenSlugExists() throws Exception {
        // given
        Article article = new Article();
        article.setTitle("Detail Test");
        article.setSlug("detail-test");
        article.setContentMd("# Detail Test");
        article.setContentHtml("<h1>Detail Test</h1>");
        article.setSummary("Summary for detail");
        article.setStatus(1);
        article.setViewCount(5);
        article.setLikeCount(2);
        article.setIsTop(0);
        articleRepository.save(article);

        // when / then
        mockMvc.perform(get("/api/articles/detail-test")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("Detail Test"))
                .andExpect(jsonPath("$.data.slug").value("detail-test"))
                .andExpect(jsonPath("$.data.contentMd").value("# Detail Test"))
                .andExpect(jsonPath("$.data.contentHtml").value("<h1>Detail Test</h1>"));
    }

    @Test
    void detail_Returns404_WhenSlugNotFound() throws Exception {
        mockMvc.perform(get("/api/articles/non-existent-slug")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // ApiResponse wraps the error with code in body
                .andExpect(jsonPath("$.code").value(2001))
                .andExpect(jsonPath("$.message").value("文章不存在"));
    }
}
