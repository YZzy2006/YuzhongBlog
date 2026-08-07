package com.ticketingsystem.yuzhonblog.service;

import com.ticketingsystem.yuzhonblog.common.BusinessException;
import com.ticketingsystem.yuzhonblog.common.ErrorCode;
import com.ticketingsystem.yuzhonblog.entity.AiConfigEntity;
import com.ticketingsystem.yuzhonblog.util.AesUtil;
import com.ticketingsystem.yuzhonblog.util.SsrfUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final AiConfigService aiConfigService;
    private final AesUtil aesUtil;

    private final WebClient webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
            .build();

    private record CacheEntry(AiConfig config, long timestamp) {}
    private volatile CacheEntry cacheEntry;
    private static final long CACHE_TTL_MS = 30_000;

    private static final Pattern SSE_CONTENT_PATTERN =
            Pattern.compile("\"content\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern SSE_REASONING_PATTERN =
            Pattern.compile("\"reasoning_content\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern ANTHROPIC_SSE_TEXT_PATTERN =
            Pattern.compile("\"text\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern ANTHROPIC_SSE_THINKING_PATTERN =
            Pattern.compile("\"thinking\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern ANTHROPIC_STOP_PATTERN =
            Pattern.compile("\"type\"\\s*:\\s*\"message_stop\"");

    private record AiConfig(String apiKey, String baseUrl, String model, int maxTokens, double temperature,
                            String authType, String apiFormat) {}

    private synchronized AiConfig getConfig() {
        long now = System.currentTimeMillis();
        CacheEntry entry = cacheEntry;
        if (entry != null && (now - entry.timestamp()) < CACHE_TTL_MS) {
            return entry.config();
        }
        AiConfigEntity entity = aiConfigService.getActive();
        if (entity == null) {
            return null;
        }
        String apiKey = null;
        try {
            apiKey = aesUtil.decrypt(entity.getApiKey());
        } catch (Exception e) {
            log.error("AI API key decryption failed", e);
            return null;
        }
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        int maxTokens = Math.max(256, Math.min(8192, entity.getMaxTokens() != null ? entity.getMaxTokens() : 4096));
        double temperature = Math.max(0, Math.min(2, entity.getTemperature() != null ? entity.getTemperature() : 0.7));

        AiConfig config = new AiConfig(apiKey, entity.getBaseUrl(), entity.getModel(),
                maxTokens, temperature,
                entity.getAuthType() != null ? entity.getAuthType() : "BEARER",
                entity.getApiFormat() != null ? entity.getApiFormat() : "OPENAI");
        cacheEntry = new CacheEntry(config, now);
        return config;
    }

    public void invalidateCache() {
        cacheEntry = null;
    }

    public boolean isConfigured() {
        return getConfig() != null;
    }

    public String chat(List<Map<String, String>> messages, String systemPrompt) {
        return chat(messages, systemPrompt, Duration.ofSeconds(120));
    }

    public String chat(List<Map<String, String>> messages, String systemPrompt, Duration timeout) {
        AiConfig config = getConfig();
        if (config == null) {
            throw new BusinessException(ErrorCode.AI_NOT_CONFIGURED);
        }

        Map<String, Object> requestBody = buildRequestBody(messages, systemPrompt, false, config);
        String uri = buildUri(config);

        try {
            log.debug("调用AI API: model={}", config.model());
            long startTime = System.currentTimeMillis();

            var chatRequest = webClient.post()
                    .uri(uri)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestBody);
            applyAuthHeader(chatRequest, config);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = chatRequest
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(timeout)
                    .block();

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("AI API调用成功: 耗时={}ms", elapsed);

            return extractContent(response, config.apiFormat());
        } catch (WebClientResponseException e) {
            log.error("AI API调用失败: status={}", e.getStatusCode());
            throw new BusinessException(ErrorCode.AI_CALL_FAILED);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI API调用异常", e);
            throw new BusinessException(ErrorCode.AI_CALL_FAILED);
        }
    }

    public void chatStream(List<Map<String, String>> messages, String systemPrompt,
                           Consumer<String> onChunk, Consumer<String> onThinking,
                           Runnable onComplete, Consumer<Throwable> onError) {
        chatStream(messages, systemPrompt, null, onChunk, onThinking, onComplete, onError);
    }

    public void chatStream(List<Map<String, String>> messages, String systemPrompt,
                           Integer maxTokensOverride,
                           Consumer<String> onChunk, Consumer<String> onThinking,
                           Runnable onComplete, Consumer<Throwable> onError) {
        AiConfig config = getConfig();
        if (config == null) {
            onError.accept(new BusinessException(ErrorCode.AI_NOT_CONFIGURED));
            return;
        }

        Map<String, Object> requestBody = buildRequestBody(messages, systemPrompt, true, config, maxTokensOverride);
        String uri = buildUri(config);
        boolean isAnthropic = "ANTHROPIC".equals(config.apiFormat());
        AtomicBoolean doneEventReceived = new AtomicBoolean(false);
        StringBuilder lineBuffer = new StringBuilder();

        log.info("调用AI流式API: model={}, format={}", config.model(), config.apiFormat());
        long startTime = System.currentTimeMillis();

        var streamRequest = webClient.post()
                .uri(uri)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(requestBody);
        applyAuthHeader(streamRequest, config);
        streamRequest.retrieve()
                .bodyToFlux(DataBuffer.class)
                .timeout(Duration.ofSeconds(120))
                .doOnComplete(() -> {
                    // Flush remaining data in lineBuffer
                    String remaining = lineBuffer.toString().trim();
                    if (!remaining.isEmpty()) {
                        if (isAnthropic) {
                            String thinking = parseAnthropicThinking(remaining);
                            if (thinking != null) onThinking.accept(thinking);
                            else {
                                String content = parseAnthropicSSEData(remaining);
                                if (content != null) onChunk.accept(content);
                            }
                        } else if (remaining.startsWith("data:")) {
                            String data = remaining.substring(5).trim();
                            if (!"[DONE]".equals(data)) {
                                String reasoning = parseSSEField(data, SSE_REASONING_PATTERN);
                                if (reasoning != null) onThinking.accept(reasoning);
                                else {
                                    String content = parseSSEField(data, SSE_CONTENT_PATTERN);
                                    if (content != null) onChunk.accept(content);
                                }
                            }
                        }
                    }
                    log.info("AI流式API完成: 耗时={}ms", System.currentTimeMillis() - startTime);
                    onComplete.run();
                })
                .doOnError(e -> {
                    log.error("AI流式API异常", e);
                    String errorMsg = "AI服务暂时不可用";
                    if (e instanceof org.springframework.web.reactive.function.client.WebClientResponseException wre) {
                        errorMsg = "AI API返回错误: HTTP " + wre.getStatusCode().value();
                    } else if (e instanceof java.util.concurrent.TimeoutException) {
                        errorMsg = "AI响应超时，请稍后重试";
                    }
                    onError.accept(new RuntimeException(errorMsg));
                })
                .subscribe(dataBuffer -> {
                    if (doneEventReceived.get()) {
                        DataBufferUtils.release(dataBuffer);
                        return;
                    }
                    String raw = dataBuffer.toString(StandardCharsets.UTF_8);
                    DataBufferUtils.release(dataBuffer);
                    lineBuffer.append(raw);
                    String buffered = lineBuffer.toString();
                    int lastNewline = buffered.lastIndexOf('\n');
                    if (lastNewline == -1) return;
                    String completePart = buffered.substring(0, lastNewline);
                    String remainder = buffered.substring(lastNewline + 1);
                    lineBuffer.setLength(0);
                    lineBuffer.append(remainder);
                    for (String line : completePart.split("\n")) {
                        line = line.trim();
                        if (isAnthropic) {
                            if (line.contains("message_stop") || ANTHROPIC_STOP_PATTERN.matcher(line).find()) {
                                doneEventReceived.set(true);
                                return;
                            }
                            String thinking = parseAnthropicThinking(line);
                            if (thinking != null) {
                                onThinking.accept(thinking);
                            } else {
                                String content = parseAnthropicSSEData(line);
                                if (content != null) {
                                    onChunk.accept(content);
                                }
                            }
                        } else {
                            if (!line.startsWith("data:")) continue;
                            String data = line.substring(5).trim();
                            if ("[DONE]".equals(data)) {
                                doneEventReceived.set(true);
                                return;
                            }
                            // Check reasoning_content first (DeepSeek / reasoning models)
                            String reasoning = parseSSEField(data, SSE_REASONING_PATTERN);
                            if (reasoning != null) {
                                onThinking.accept(reasoning);
                            } else {
                                String content = parseSSEField(data, SSE_CONTENT_PATTERN);
                                if (content != null) {
                                    onChunk.accept(content);
                                }
                            }
                        }
                    }
                });
    }

    public Map<String, Object> testConnection() {
        AiConfig config = getConfig();
        if (config == null) {
            return Map.of("success", false, "message", "AI服务未配置");
        }
        return doTestConnection(config);
    }

    public Map<String, Object> testConnection(String baseUrl, String apiKey, String authType,
                                              String apiFormat, String model, Integer maxTokens, Double temperature) {
        int mt = Math.max(256, Math.min(8192, maxTokens != null ? maxTokens : 4096));
        double temp = Math.max(0, Math.min(2, temperature != null ? temperature : 0.7));
        AiConfig config = new AiConfig(apiKey, baseUrl, model, mt, temp,
                authType != null ? authType : "BEARER",
                apiFormat != null ? apiFormat : "OPENAI");
        return doTestConnection(config);
    }

    private Map<String, Object> doTestConnection(AiConfig config) {

        Map<String, Object> requestBody = buildRequestBody(
                List.of(Map.of("role", "user", "content", "Hi")),
                "Reply with exactly: OK", false, config);
        String uri = buildUri(config);

        try {
            long startTime = System.currentTimeMillis();

            @SuppressWarnings("unchecked")
            var testRequest = webClient.post()
                    .uri(uri)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestBody);
            applyAuthHeader(testRequest, config);
            Map<String, Object> response = testRequest
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            long elapsed = System.currentTimeMillis() - startTime;

            String modelUsed = response.get("model") != null ? response.get("model").toString() : config.model();
            return Map.of(
                    "success", true,
                    "message", "连接成功",
                    "latency", elapsed,
                    "model", modelUsed
            );
        } catch (WebClientResponseException e) {
            log.error("AI连接测试失败: status={}", e.getStatusCode());
            return Map.of("success", false, "message", "连接失败: HTTP " + e.getStatusCode());
        } catch (Exception e) {
            log.error("AI连接测试异常", e);
            return Map.of("success", false, "message", "连接失败，请检查配置");
        }
    }

    private Map<String, Object> buildRequestBody(List<Map<String, String>> messages,
                                                  String systemPrompt, boolean stream, AiConfig config) {
        return buildRequestBody(messages, systemPrompt, stream, config, null);
    }

    private Map<String, Object> buildRequestBody(List<Map<String, String>> messages,
                                                  String systemPrompt, boolean stream, AiConfig config,
                                                  Integer maxTokensOverride) {
        boolean isAnthropic = "ANTHROPIC".equals(config.apiFormat());
        List<Map<String, String>> apiMessages = new ArrayList<>();
        for (Map<String, String> msg : messages) {
            apiMessages.add(Map.of(
                    "role", msg.getOrDefault("role", "user"),
                    "content", msg.getOrDefault("content", "")
            ));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.model());
        int effectiveMaxTokens = maxTokensOverride != null && maxTokensOverride > 0
                ? maxTokensOverride : config.maxTokens();
        body.put("max_tokens", effectiveMaxTokens);
        body.put("temperature", config.temperature());
        if (isAnthropic) {
            // Anthropic: system prompt goes in top-level "system" field
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                body.put("system", systemPrompt);
            }
            body.put("messages", apiMessages);
            body.put("stream", stream);
        } else {
            // OpenAI: system prompt as a message with role "system"
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                apiMessages.add(0, Map.of("role", "system", "content", systemPrompt));
            }
            body.put("messages", apiMessages);
            body.put("stream", stream);
        }
        return body;
    }

    private String buildUri(AiConfig config) {
        String base = config.baseUrl();
        try {
            SsrfUtil.validateUrl(base);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "AI Base URL 不合法");
        }
        if (!base.endsWith("/")) base += "/";
        if ("ANTHROPIC".equals(config.apiFormat())) {
            return base + "messages";
        }
        return base + "chat/completions";
    }

    private String parseSSEField(String data, Pattern pattern) {
        if (data == null || data.isBlank()) return null;
        try {
            Matcher matcher = pattern.matcher(data);
            if (matcher.find()) {
                return unescapeJson(matcher.group(1));
            }
        } catch (Exception e) {
            log.debug("SSE data parse failed: {}", data, e);
        }
        return null;
    }

    private String parseAnthropicSSEData(String line) {
        return parseAnthropicField(line, ANTHROPIC_SSE_TEXT_PATTERN);
    }

    private String parseAnthropicThinking(String line) {
        return parseAnthropicField(line, ANTHROPIC_SSE_THINKING_PATTERN);
    }

    private String parseAnthropicField(String line, Pattern pattern) {
        if (line == null || line.isBlank()) return null;
        try {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                return unescapeJson(matcher.group(1));
            }
        } catch (Exception e) {
            log.debug("Anthropic SSE data parse failed: {}", line, e);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response, String apiFormat) {
        if (response == null || response.isEmpty()) {
            return "抱歉，AI暂时无法回复。";
        }
        if ("ANTHROPIC".equals(apiFormat)) {
            // Anthropic: {"content": [{"type": "text", "text": "..."}]}
            Object contentObj = response.get("content");
            if (contentObj instanceof List<?> contentList && !contentList.isEmpty()) {
                Object first = contentList.get(0);
                if (first instanceof Map<?, ?> contentBlock) {
                    String text = (String) contentBlock.get("text");
                    if (text != null) return text;
                }
            }
        } else {
            // OpenAI: {"choices": [{"message": {"content": "..."}}]}
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                if (message != null) {
                    return (String) message.get("content");
                }
            }
        }
        return "抱歉，AI暂时无法回复。";
    }

    private String unescapeJson(String s) {
        return s.replace("\\\\", "\0")       // placeholder for literal backslash
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\r", "\r")
                .replace("\\\"", "\"")
                .replace("\0", "\\");         // restore literal backslash
    }

    private void applyAuthHeader(WebClient.RequestHeadersSpec<?> request, AiConfig config) {
        switch (config.authType()) {
            case "X_API_KEY" -> request.header("x-api-key", config.apiKey());
            case "RAW_TOKEN" -> request.header(HttpHeaders.AUTHORIZATION, config.apiKey());
            case "API_KEY" -> request.header("api-key", config.apiKey());
            default -> request.header(HttpHeaders.AUTHORIZATION, "Bearer " + config.apiKey());
        }
        if ("ANTHROPIC".equals(config.apiFormat())) {
            request.header("anthropic-version", "2023-06-01");
        }
    }

}
