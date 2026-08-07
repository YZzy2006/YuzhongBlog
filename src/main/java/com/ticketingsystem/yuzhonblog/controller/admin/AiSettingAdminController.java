package com.ticketingsystem.yuzhonblog.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.dto.ai.AiChatRequest;
import com.ticketingsystem.yuzhonblog.dto.ai.AiConfigRequest;
import com.ticketingsystem.yuzhonblog.security.RequirePermission;
import jakarta.validation.Valid;
import com.ticketingsystem.yuzhonblog.entity.AiConfigEntity;
import com.ticketingsystem.yuzhonblog.service.AiConfigService;
import com.ticketingsystem.yuzhonblog.service.AiService;
import com.ticketingsystem.yuzhonblog.service.BalanceCheckService;
import com.ticketingsystem.yuzhonblog.util.AesUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@RestController
@RequestMapping("/admin/ai")
@RequiredArgsConstructor
@Slf4j
public class AiSettingAdminController {

    private final AiConfigService aiConfigService;
    private final AiService aiService;
    private final BalanceCheckService balanceCheckService;
    private final AesUtil aesUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== Editor Stream ====================

    @PostMapping(value = "/editor/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequirePermission("setting:edit")
    public Flux<String> editorStream(@Valid @RequestBody AiChatRequest request) {
        request.validateMessages();
        List<Map<String, String>> messages = request.getMessages();
        if (messages == null || messages.isEmpty()) {
            messages = List.of(Map.of("role", "user", "content", request.getMessage()));
        }
        String systemPrompt = request.getSystemPrompt();
        Integer maxTokens = request.getMaxTokens();

        List<Map<String, String>> finalMessages = messages;
        return Flux.create(sink -> {
            Consumer<String> onChunk = chunk -> {
                try {
                    sink.next(objectMapper.writeValueAsString(Map.of("type", "chunk", "content", chunk)));
                } catch (Exception e) {
                    log.warn("Failed to send SSE chunk", e);
                }
            };

            Consumer<String> onThinking = thinking -> {
                try {
                    sink.next(objectMapper.writeValueAsString(Map.of("type", "thinking", "content", thinking)));
                } catch (Exception e) {
                    log.warn("Failed to send SSE thinking", e);
                }
            };

            Runnable onComplete = () -> {
                sink.next("{\"type\":\"done\"}");
                sink.complete();
            };

            Consumer<Throwable> onError = e -> {
                log.error("AI editor stream error", e);
                try {
                    sink.next(objectMapper.writeValueAsString(Map.of("type", "error", "content", "AI服务暂时不可用")));
                } catch (Exception ignored) {
                }
                sink.complete();
            };

            aiService.chatStream(
                    finalMessages, systemPrompt, maxTokens,
                    onChunk, onThinking, onComplete, onError
            );
        });
    }

    // ==================== Config CRUD ====================

    @GetMapping("/configs")
    @RequirePermission("setting:view")
    public ApiResponse<List<Map<String, Object>>> listConfigs() {
        List<AiConfigEntity> configs = aiConfigService.list();
        List<Map<String, Object>> result = configs.stream().map(this::toMaskedResponse).toList();
        return ApiResponse.success(result);
    }

    @GetMapping("/configs/{id}")
    @RequirePermission("setting:view")
    public ApiResponse<Map<String, Object>> getConfig(@PathVariable Long id) {
        AiConfigEntity entity = aiConfigService.get(id);
        return ApiResponse.success(toMaskedResponse(entity));
    }

    @PostMapping("/configs")
    @RequirePermission("setting:edit")
    public ApiResponse<Map<String, Object>> createConfig(@Valid @RequestBody AiConfigRequest request) {
        AiConfigEntity entity = aiConfigService.create(request);
        aiService.invalidateCache();
        log.info("AI配置已创建: id={}", entity.getId());
        return ApiResponse.success(toMaskedResponse(entity));
    }

    @PutMapping("/configs/{id}")
    @RequirePermission("setting:edit")
    public ApiResponse<Map<String, Object>> updateConfig(@PathVariable Long id, @Valid @RequestBody AiConfigRequest request) {
        AiConfigEntity entity = aiConfigService.update(id, request);
        aiService.invalidateCache();
        log.info("AI配置已更新: id={}", id);
        return ApiResponse.success(toMaskedResponse(entity));
    }

    @DeleteMapping("/configs/{id}")
    @RequirePermission("setting:edit")
    public ApiResponse<Void> deleteConfig(@PathVariable Long id) {
        aiConfigService.delete(id);
        aiService.invalidateCache();
        log.info("AI配置已删除: id={}", id);
        return ApiResponse.success();
    }

    @PutMapping("/configs/{id}/activate")
    @RequirePermission("setting:edit")
    public ApiResponse<Void> activateConfig(@PathVariable Long id) {
        aiConfigService.activate(id);
        aiService.invalidateCache();
        log.info("AI配置已激活: id={}", id);
        return ApiResponse.success();
    }

    // ==================== Test & Balance ====================

    @PostMapping("/configs/{id}/test")
    @RequirePermission("setting:edit")
    public ApiResponse<Map<String, Object>> testConnection(@PathVariable Long id) {
        AiConfigEntity entity = aiConfigService.get(id);
        String apiKey = decryptKey(entity);
        if (apiKey.isBlank()) {
            return ApiResponse.success(Map.of("success", false, "message", "API Key 未配置"));
        }
        Map<String, Object> result = aiService.testConnection(entity.getBaseUrl(), apiKey,
                entity.getAuthType(), entity.getApiFormat(), entity.getModel(),
                entity.getMaxTokens(), entity.getTemperature());
        log.info("AI connection test: id={}, success={}", id, result.get("success"));
        return ApiResponse.success(result);
    }

    @GetMapping("/configs/{id}/balance")
    @RequirePermission("setting:view")
    public ApiResponse<BalanceCheckService.BalanceInfo> checkBalance(@PathVariable Long id) {
        AiConfigEntity entity = aiConfigService.get(id);
        String apiKey = decryptKey(entity);
        if (apiKey.isBlank()) {
            return ApiResponse.success(BalanceCheckService.BalanceInfo.fail("未配置 API Key"));
        }
        BalanceCheckService.BalanceInfo info = balanceCheckService.check(
                entity.getBaseUrl(), apiKey, entity.getAuthType(),
                entity.getBalanceUrl(), entity.getBalanceScript());
        log.info("Balance check: id={}, success={}", id, info.success());
        return ApiResponse.success(info);
    }

    // ==================== Legacy: global AI enabled flag ====================

    @GetMapping("/settings")
    @RequirePermission("setting:view")
    public ApiResponse<Map<String, String>> getSettings() {
        Map<String, String> result = new HashMap<>();
        result.put("ai_enabled", String.valueOf(aiConfigService.getActive() != null));
        return ApiResponse.success(result);
    }

    // ==================== Helpers ====================

    private Map<String, Object> toMaskedResponse(AiConfigEntity entity) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", entity.getId());
        map.put("name", entity.getName());
        map.put("apiKey", maskApiKey(decryptKey(entity)));
        map.put("baseUrl", entity.getBaseUrl());
        map.put("model", entity.getModel());
        map.put("maxTokens", entity.getMaxTokens());
        map.put("temperature", entity.getTemperature());
        map.put("apiFormat", entity.getApiFormat());
        map.put("authType", entity.getAuthType());
        map.put("websiteUrl", entity.getWebsiteUrl());
        map.put("balanceUrl", entity.getBalanceUrl());
        map.put("balanceScript", entity.getBalanceScript());
        map.put("description", entity.getDescription());
        map.put("isActive", entity.getIsActive());
        map.put("sortOrder", entity.getSortOrder());
        map.put("createdAt", entity.getCreatedAt());
        map.put("updatedAt", entity.getUpdatedAt());
        return map;
    }

    private String decryptKey(AiConfigEntity entity) {
        try {
            String decrypted = aesUtil.decrypt(entity.getApiKey());
            return decrypted != null ? decrypted : "";
        } catch (Exception e) {
            return "";
        }
    }

    private static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 4) return "****";
        return "****" + apiKey.substring(apiKey.length() - 4);
    }
}
