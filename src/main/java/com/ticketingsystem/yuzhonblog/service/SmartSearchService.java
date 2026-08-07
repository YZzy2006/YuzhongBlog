package com.ticketingsystem.yuzhonblog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketingsystem.yuzhonblog.dto.ai.SmartSearchResult;
import com.ticketingsystem.yuzhonblog.entity.Category;
import com.ticketingsystem.yuzhonblog.entity.Tag;
import com.ticketingsystem.yuzhonblog.repository.CategoryRepository;
import com.ticketingsystem.yuzhonblog.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmartSearchService {

    private final AiService aiService;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Pattern JSON_PATTERN = Pattern.compile("\\{[^{}]*}");

    private static final String SYSTEM_PROMPT = """
            你是一个技术博客的搜索解析器。用户会输入自然语言搜索请求，你需要将其解析为结构化的JSON参数。

            可用参数：
            - keyword: 搜索关键词（字符串，从用户描述中提取核心技术词汇）
            - categoryId: 分类ID（数字，根据用户提到的分类名称匹配）
            - tagId: 标签ID（数字，根据用户提到的标签名称匹配）
            - sortBy: 排序方式（"newest"最新发布, "views"最多浏览, "likes"最多点赞）

            规则：
            1. 只返回JSON，不要任何解释文字
            2. 如果某个参数无法确定，不要包含该字段
            3. keyword 应提取核心技术词汇，去掉无关修饰词
            4. 如果用户提到"最新"、"最近"，sortBy 设为 "newest"
            5. 如果用户提到"热门"、"最多阅读"、"浏览最多"，sortBy 设为 "views"
            6. 如果用户提到"最多点赞"、"最受欢迎"，sortBy 设为 "likes"
            7. 只返回JSON对象，格式如：{"keyword":"Spring Boot","sortBy":"newest"}

            示例：
            用户："找一下关于Vue3的文章" → {"keyword":"Vue3"}
            用户："最新的Java文章" → {"keyword":"Java","sortBy":"newest"}
            用户："热门的前端文章" → {"keyword":"前端","sortBy":"views"}
            用户："Spring Boot分类下的文章" → {"keyword":"Spring Boot"}
            """;

    public SmartSearchResult parse(String query) {
        SmartSearchResult result = new SmartSearchResult();

        if (!aiService.isConfigured()) {
            log.warn("AI未配置，使用降级解析");
            result.setKeyword(query.trim());
            return result;
        }

        try {
            String response = aiService.chat(
                    List.of(Map.of("role", "user", "content", query)),
                    SYSTEM_PROMPT
            );

            if (response == null || response.isBlank()) {
                result.setKeyword(query.trim());
                return result;
            }

            // Extract JSON from response (strip markdown fences if present)
            String json = extractJson(response);
            if (json == null) {
                result.setKeyword(query.trim());
                return result;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(json, Map.class);

            if (parsed.containsKey("keyword") && parsed.get("keyword") instanceof String kw && !kw.isBlank()) {
                result.setKeyword(kw);
            }
            if (parsed.containsKey("sortBy") && parsed.get("sortBy") instanceof String sb) {
                if ("newest".equals(sb) || "views".equals(sb) || "likes".equals(sb)) {
                    result.setSortBy(sb);
                }
            }

            // Match category/tag by name if AI returned them as names
            if (parsed.containsKey("categoryId") && parsed.get("categoryId") instanceof Number n) {
                result.setCategoryId(n.longValue());
            }
            if (parsed.containsKey("tagId") && parsed.get("tagId") instanceof Number n) {
                result.setTagId(n.longValue());
            }

            // Fuzzy match: if AI mentioned a category/tag name in keyword, try to resolve
            if (result.getCategoryId() == null && result.getTagId() == null && result.getKeyword() != null) {
                resolveCategoryAndTag(result);
            }

            log.debug("AI搜索解析: query='{}' → keyword={}, categoryId={}, tagId={}, sortBy={}",
                    query, result.getKeyword(), result.getCategoryId(), result.getTagId(), result.getSortBy());

        } catch (Exception e) {
            log.warn("AI搜索解析失败，降级为关键词搜索", e);
            result.setKeyword(query.trim());
        }

        return result;
    }

    private void resolveCategoryAndTag(SmartSearchResult result) {
        String kw = result.getKeyword().toLowerCase();
        List<Category> categories = categoryRepository.findAllByOrderBySortOrderDesc();
        for (Category cat : categories) {
            if (kw.contains(cat.getName().toLowerCase()) || cat.getName().toLowerCase().contains(kw)) {
                result.setCategoryId(cat.getId());
                // Remove category name from keyword
                String cleaned = result.getKeyword().replaceAll("(?i)" + Pattern.quote(cat.getName()), "").trim();
                result.setKeyword(cleaned.isEmpty() ? null : cleaned);
                return;
            }
        }
        List<Tag> tags = tagRepository.findAllByOrderByNameAsc();
        for (Tag tag : tags) {
            if (kw.contains(tag.getName().toLowerCase()) || tag.getName().toLowerCase().contains(kw)) {
                result.setTagId(tag.getId());
                String cleaned = result.getKeyword().replaceAll("(?i)" + Pattern.quote(tag.getName()), "").trim();
                result.setKeyword(cleaned.isEmpty() ? null : cleaned);
                return;
            }
        }
    }

    private String extractJson(String text) {
        // Try to find JSON object in the response
        Matcher matcher = JSON_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        // Try without fences
        String trimmed = text.strip();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }
        return null;
    }
}
