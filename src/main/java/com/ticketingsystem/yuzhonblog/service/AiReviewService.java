package com.ticketingsystem.yuzhonblog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiReviewService {

    private final AiService aiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Duration REVIEW_TIMEOUT = Duration.ofSeconds(30);

    private static final String REVIEW_SYSTEM_PROMPT = """
            你是一个技术博客平台的内容审核助手。你需要对提交发布的博客内容进行语义审核。

            审核标准：
            1. 内容是否包含违法违规信息（政治敏感、色情、赌博、诈骗等）
            2. 内容是否包含垃圾广告、引流信息
            3. 内容是否包含明显的错误信息或误导性内容（如虚假技术声明）
            4. 内容质量是否极低（如纯乱码、无意义内容）

            注意：
            - 技术文章中讨论安全攻防、漏洞分析等属于正常技术内容，不应标记
            - 代码示例中的敏感词（如变量名包含敏感词）不应标记
            - 正常的技术讨论和观点表达不应标记
            - 只有明显违规或有问题的内容才应该被标记

            你必须返回严格的JSON格式，不要包含任何其他文字：
            {"pass": true, "reason": ""}
            或
            {"pass": false, "reason": "具体说明问题所在"}

            reason 字段在 pass=true 时为空字符串，在 pass=false 时简要说明原因（50字以内）。
            """;

    /**
     * Review content via AI. Returns the parsed review result.
     * Fail-open: returns pass=true on any error.
     */
    public AiReviewResult review(String contentType, Long contentId, String title, String bodyText) {
        if (!aiService.isConfigured()) {
            log.info("AI未配置，跳过内容审核，直接通过");
            return AiReviewResult.pass();
        }

        String contentToReview = buildReviewContent(contentType, title, bodyText);
        List<Map<String, String>> messages = List.of(
                Map.of("role", "user", "content", contentToReview)
        );

        try {
            String response = aiService.chat(messages, REVIEW_SYSTEM_PROMPT, REVIEW_TIMEOUT);
            return parseResponse(response);
        } catch (Exception e) {
            log.warn("AI审核调用失败，fail-open直接通过: contentType={}, contentId={}, error={}",
                    contentType, contentId, e.getMessage());
            return AiReviewResult.pass();
        }
    }

    private String buildReviewContent(String contentType, String title, String bodyText) {
        String truncated = bodyText != null && bodyText.length() > 3000
                ? bodyText.substring(0, 3000) + "...(已截断)" : (bodyText != null ? bodyText : "");
        return "[%s] 标题: %s\n\n内容:\n%s".formatted(contentType, title != null ? title : "", truncated);
    }

    @SuppressWarnings("unchecked")
    private AiReviewResult parseResponse(String response) {
        if (response == null || response.isBlank()) {
            return AiReviewResult.pass();
        }
        try {
            String json = extractJson(response);
            Map<String, Object> parsed = objectMapper.readValue(json, Map.class);
            boolean pass = Boolean.TRUE.equals(parsed.get("pass"));
            String reason = parsed.get("reason") instanceof String r ? r : "";
            return pass ? AiReviewResult.pass() : AiReviewResult.flag(reason);
        } catch (Exception e) {
            log.warn("AI审核响应解析失败，fail-open: {}", response, e);
            return AiReviewResult.pass();
        }
    }

    private String extractJson(String text) {
        String trimmed = text.strip();
        // Try markdown fences
        Matcher m = Pattern.compile("```(?:json)?\\s*(\\{.*?})\\s*```", Pattern.DOTALL).matcher(trimmed);
        if (m.find()) return m.group(1);
        // Find outermost JSON object by brace matching
        int start = trimmed.indexOf('{');
        if (start >= 0) {
            int depth = 0;
            for (int i = start; i < trimmed.length(); i++) {
                if (trimmed.charAt(i) == '{') depth++;
                else if (trimmed.charAt(i) == '}') depth--;
                if (depth == 0) return trimmed.substring(start, i + 1);
            }
        }
        return trimmed;
    }

    public record AiReviewResult(boolean approved, String reason) {
        public static AiReviewResult pass() { return new AiReviewResult(true, ""); }
        public static AiReviewResult flag(String reason) { return new AiReviewResult(false, reason); }
    }
}
