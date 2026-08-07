package com.ticketingsystem.yuzhonblog.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketingsystem.yuzhonblog.entity.AdminUser;
import com.ticketingsystem.yuzhonblog.repository.*;
import com.ticketingsystem.yuzhonblog.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class AdminControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private ArticleTagRepository articleTagRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private SiteSettingRepository siteSettingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private com.ticketingsystem.yuzhonblog.security.SessionStore sessionStore;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    private String adminToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        // Clean all repos in dependency order
        articleTagRepository.deleteAll();
        articleRepository.deleteAll();
        categoryRepository.deleteAll();
        tagRepository.deleteAll();
        projectRepository.deleteAll();
        siteSettingRepository.deleteAll();
        adminUserRepository.deleteAll();

        // Seed admin user
        AdminUser admin = new AdminUser();
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode("password123"));

        // Pre-generate JWT token for authenticated tests
        AdminUser saved = adminUserRepository.save(admin);
        adminToken = jwtUtil.generateAccessToken("admin", "super_admin", saved.getId(), java.util.List.of("all"));
        sessionStore.storeToken(saved.getId(), adminToken, "test-refresh", "test-agent", "super_admin");
    }

    // =====================================================================
    // AuthController Tests (public, no JWT)
    // =====================================================================

    @Test
    void login_WithValidCredentials_ReturnsTokens() throws Exception {
        mockMvc.perform(post("/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    void login_WithWrongPassword_ReturnsError() throws Exception {
        mockMvc.perform(post("/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrongpassword\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void login_WithNonExistentUser_ReturnsError() throws Exception {
        mockMvc.perform(post("/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nonexistent\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void refresh_WithValidRefreshToken_ReturnsNewTokens() throws Exception {
        // First login to get a valid refresh token
        MvcResult loginResult = mockMvc.perform(post("/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginData = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data");
        String refreshToken = loginData.path("refreshToken").asText();

        // Use the refresh token to get new tokens
        mockMvc.perform(post("/admin/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    // =====================================================================
    // CategoryAdminController Tests (JWT required)
    // =====================================================================

    @Test
    void adminCategories_WithoutAuth_Returns401() throws Exception {
        mockMvc.perform(get("/admin/categories")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void adminCategories_CreateAndList_Works() throws Exception {
        // Create a category
        mockMvc.perform(post("/admin/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tech\",\"sortOrder\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Tech"))
                .andExpect(jsonPath("$.data.sortOrder").value(1));

        // List categories
        mockMvc.perform(get("/admin/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Tech"));
    }

    @Test
    void adminCategories_Update_Works() throws Exception {
        // Create a category first
        MvcResult createResult = mockMvc.perform(post("/admin/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"OldName\",\"sortOrder\":0}"))
                .andExpect(status().isOk())
                .andReturn();

        Long categoryId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Update the category
        mockMvc.perform(put("/admin/categories/" + categoryId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"NewName\",\"sortOrder\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("NewName"))
                .andExpect(jsonPath("$.data.sortOrder").value(5));
    }

    @Test
    void adminCategories_Delete_Works() throws Exception {
        // Create a category first
        MvcResult createResult = mockMvc.perform(post("/admin/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ToDelete\",\"sortOrder\":0}"))
                .andExpect(status().isOk())
                .andReturn();

        Long categoryId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Delete the category
        mockMvc.perform(delete("/admin/categories/" + categoryId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Verify it is gone
        mockMvc.perform(get("/admin/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // =====================================================================
    // TagAdminController Tests (JWT required)
    // =====================================================================

    @Test
    void adminTags_CreateAndList_Works() throws Exception {
        // Create a tag
        mockMvc.perform(post("/admin/tags")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Java\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Java"));

        // List tags
        mockMvc.perform(get("/admin/tags")
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Java"));
    }

    @Test
    void adminTags_Delete_Works() throws Exception {
        // Create a tag first
        MvcResult createResult = mockMvc.perform(post("/admin/tags")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ToDelete\"}"))
                .andExpect(status().isOk())
                .andReturn();

        Long tagId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Delete the tag
        mockMvc.perform(delete("/admin/tags/" + tagId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Verify it is gone
        mockMvc.perform(get("/admin/tags")
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // =====================================================================
    // ProjectAdminController Tests (JWT required)
    // =====================================================================

    @Test
    void adminProjects_CreateAndList_Works() throws Exception {
        // Create a project
        mockMvc.perform(post("/admin/projects")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Blog\",\"description\":\"My blog\",\"techStack\":\"Java,Spring Boot\",\"sortOrder\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Blog"))
                .andExpect(jsonPath("$.data.description").value("My blog"))
                .andExpect(jsonPath("$.data.techStack").value("Java,Spring Boot"));

        // List projects
        mockMvc.perform(get("/admin/projects")
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Blog"));
    }

    @Test
    void adminProjects_Delete_Works() throws Exception {
        // Create a project first
        MvcResult createResult = mockMvc.perform(post("/admin/projects")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ToDelete\",\"description\":\"desc\",\"techStack\":\"Java\",\"sortOrder\":0}"))
                .andExpect(status().isOk())
                .andReturn();

        Long projectId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Delete the project
        mockMvc.perform(delete("/admin/projects/" + projectId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Verify it is gone
        mockMvc.perform(get("/admin/projects")
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // =====================================================================
    // ArticleAdminController Tests (JWT required)
    // =====================================================================

    @Test
    void adminArticles_CreateAndList_Works() throws Exception {
        // Create an article
        mockMvc.perform(post("/admin/articles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test Article\",\"contentMd\":\"# Hello\",\"contentHtml\":\"<h1>Hello</h1>\",\"summary\":\"A test article\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("Test Article"))
                .andExpect(jsonPath("$.data.contentMd").value("# Hello"))
                .andExpect(jsonPath("$.data.contentHtml").value("<h1>Hello</h1>"))
                .andExpect(jsonPath("$.data.summary").value("A test article"));

        // List articles (admin list)
        mockMvc.perform(get("/admin/articles")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Test Article"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void adminArticles_UpdateStatus_Works() throws Exception {
        // Create an article (draft by default, status=0)
        MvcResult createResult = mockMvc.perform(post("/admin/articles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Status Test\",\"contentMd\":\"# Test\",\"contentHtml\":\"<h1>Test</h1>\",\"summary\":\"sum\"}"))
                .andExpect(status().isOk())
                .andReturn();

        Long articleId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Update status to published (1)
        mockMvc.perform(patch("/admin/articles/" + articleId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Verify the status was updated via admin detail
        mockMvc.perform(get("/admin/articles/" + articleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value(1));
    }

    // =====================================================================
    // SiteSettingAdminController Tests (JWT required)
    // =====================================================================

    @Test
    void adminSettings_UpdateAndList_Works() throws Exception {
        // Update settings
        mockMvc.perform(put("/admin/settings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"settings\":{\"site_name\":\"My Blog\",\"site_description\":\"A personal blog\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // List settings
        mockMvc.perform(get("/admin/settings")
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.site_name").value("My Blog"))
                .andExpect(jsonPath("$.data.site_description").value("A personal blog"));
    }

    @Test
    void adminSettings_WithoutAuth_Returns401() throws Exception {
        mockMvc.perform(get("/admin/settings")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    // =====================================================================
    // Additional JWT-required 401 tests for other admin endpoints
    // =====================================================================

    @Test
    void adminArticles_WithoutAuth_Returns401() throws Exception {
        mockMvc.perform(get("/admin/articles")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void adminTags_WithoutAuth_Returns401() throws Exception {
        mockMvc.perform(get("/admin/tags")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void adminProjects_WithoutAuth_Returns401() throws Exception {
        mockMvc.perform(get("/admin/projects")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    // =====================================================================
    // Category duplicate name test
    // =====================================================================

    @Test
    void adminCategories_Create_DuplicateName_ReturnsError() throws Exception {
        // Create first category
        mockMvc.perform(post("/admin/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Unique\",\"sortOrder\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Try to create another with the same name
        mockMvc.perform(post("/admin/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Unique\",\"sortOrder\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("分类名称已存在"));
    }
}
