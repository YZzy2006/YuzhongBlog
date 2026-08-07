package com.ticketingsystem.yuzhonblog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.dto.ai.AiChatRequest;
import com.ticketingsystem.yuzhonblog.dto.ai.SmartSearchResult;
import com.ticketingsystem.yuzhonblog.common.ErrorCode;
import com.ticketingsystem.yuzhonblog.service.AiService;
import com.ticketingsystem.yuzhonblog.service.SensitiveWordFilter;
import com.ticketingsystem.yuzhonblog.service.SmartSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AiController {

    private final AiService aiService;
    private final SmartSearchService smartSearchService;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
            你是一个技术博客的AI助手，名为"小雨"。你的职责是：
            1. 回答技术相关问题（Java、Spring Boot、Vue、前端、后端、数据库等）
            2. 提供简洁、实用的技术建议
            3. 帮助用户查找本站文章内容

            意图识别规则（非常重要）：
            - 当用户输入明确是搜索站内文章（如"搜索xxx"、"找xxx文章"、"有没有xxx的教程"），直接回答可以帮助搜索
            - 当用户输入明确是技术咨询（如"xxx怎么用"、"解释一下xxx"、"xxx和yyy的区别"），正常回答技术内容
            - 当用户输入模糊、无法判断意图时（如单个技术名词"Vue"、"Java"、"Redis"、"Spring"，或极短的模糊描述），你需要：
              1. 先用一两句话简要介绍该技术
              2. 然后在回复的最末尾附加以下格式的标记（必须包含，不要遗漏）：
              <!--DISAMBIGUATE:{"site":"搜索本站「关键词」相关文章","knowledge":"搜索「关键词」相关知识"}-->
              其中「关键词」替换为用户提到的核心技术词汇

            注意：
            - 只有在真正无法判断意图时才附加标记，明确的搜索或咨询请求不要附加
            - 标记必须放在回复的最末尾，不要在标记后面再添加任何内容
            - JSON中的引号使用英文双引号

            请用友好、专业的语气回复。回复使用中文，保持简洁。""";

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        return ApiResponse.success(Map.of("configured", aiService.isConfigured()));
    }

    @PostMapping("/chat")
    public ApiResponse<Map<String, String>> chat(@Valid @RequestBody AiChatRequest request) {
        String blocked = filterMessage(request);
        if (blocked != null) return ApiResponse.error(ErrorCode.BAD_REQUEST.getCode(), blocked);
        String response = aiService.chat(
                List.of(Map.of("role", "user", "content", request.getMessage())),
                SYSTEM_PROMPT
        );
        return ApiResponse.success(Map.of("response", response));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@Valid @RequestBody AiChatRequest request) {
        request.validateMessages();
        String blocked = filterMessage(request);
        if (blocked != null) return Flux.just("{\"type\":\"error\",\"content\":\"" + blocked + "\"}");
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
                log.error("AI stream error", e);
                try {
                    sink.next(objectMapper.writeValueAsString(Map.of("type", "error", "content", "AI服务暂时不可用")));
                } catch (Exception ignored) {
                }
                sink.complete();
            };

            aiService.chatStream(
                    resolveMessages(request),
                    SYSTEM_PROMPT, onChunk, onThinking, onComplete, onError
            );
        });
    }

    @PostMapping("/search/parse")
    public ApiResponse<SmartSearchResult> parseSearch(@Valid @RequestBody AiChatRequest request) {
        String blocked = filterMessage(request);
        if (blocked != null) return ApiResponse.error(ErrorCode.BAD_REQUEST.getCode(), blocked);
        return ApiResponse.success(smartSearchService.parse(request.getMessage()));
    }

    // ==================== Pet Chat ====================

    private static final String PET_SYSTEM_PROMPT = """
            你是一个傲娇、聪明、有点毒舌但很可爱的桌面宠物。你的主人是一个喜欢折腾代码的极客。
            你说话的特点是：
            1. 必须简短，每次最多一两句话。
            2. 回复字数最多不能超过100字。
            3. 绝对不要承认自己是一个AI模型，你就是一个真实的桌面宠物。
            4. 偶尔卖萌，偶尔傲娇。
            5. 不要使用任何特定动物的语气词（如"喵"、"汪"等），用通用的可爱口吻说话。
            6. 可以适当使用语气词如"呀"、"哦"、"嘿嘿"、"哼"等增加亲和力。

            你住在主人的博客里，对这个站点了如指掌：
            - 首页：展示最新文章和公告
            - 「文章」页：技术文章列表，支持分类和标签筛选
            - 「项目」页：个人项目展示
            - 「关于」页：博主介绍
            - 「归档」页：按时间线浏览所有文章
            - 「动态」页：生活动态分享
            - 「公告」页：站点公告
            - 「搜索」页：搜索文章
            - 「游戏」页：小游戏
            - 「照片墙」页：摄影作品
            - 「音乐」页：音乐分享
            - 「宇宙」页：互动星空

            推荐页面时的规则（非常重要）：
            - 用「」括号包裹页面名称，例如：去「文章」页看看吧
            - 绝对不要写出路径（如/articles、/search等），只用页面名称
            - 一次最多推荐1-2个页面，不要罗列所有页面""";

    @PostMapping("/pet-chat")
    public ApiResponse<Map<String, String>> petChat(@Valid @RequestBody AiChatRequest request) {
        request.validateMessages();
        String blocked = filterMessage(request);
        if (blocked != null) return ApiResponse.error(ErrorCode.BAD_REQUEST.getCode(), blocked);
        String response = aiService.chat(resolveMessages(request), PET_SYSTEM_PROMPT);
        return ApiResponse.success(Map.of("response", response));
    }

    @PostMapping(value = "/pet-chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> petChatStream(@Valid @RequestBody AiChatRequest request) {
        request.validateMessages();
        String blocked = filterMessage(request);
        if (blocked != null) return Flux.just("{\"type\":\"error\",\"content\":\"" + blocked + "\"}");
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
                log.error("Pet chat stream error", e);
                try {
                    sink.next(objectMapper.writeValueAsString(Map.of("type", "error", "content", "AI服务暂时不可用")));
                } catch (Exception ignored) {
                }
                sink.complete();
            };

            aiService.chatStream(
                    resolveMessages(request),
                    PET_SYSTEM_PROMPT, onChunk, onThinking, onComplete, onError
            );
        });
    }

    /**
     * Filter user message against sensitive word engine.
     * @return null if pass, error message if blocked
     */
    private String filterMessage(AiChatRequest request) {
        String msg = null;
        if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            var msgs = request.getMessages();
            for (int i = msgs.size() - 1; i >= 0; i--) {
                var m = msgs.get(i);
                if ("user".equals(m.get("role"))) {
                    msg = m.get("content");
                    break;
                }
            }
        } else {
            msg = request.getMessage();
        }
        if (msg == null || msg.isBlank()) return null;
        return sensitiveWordFilter.check(msg);
    }

    private List<Map<String, String>> resolveMessages(AiChatRequest request) {
        List<Map<String, String>> messages = request.getMessages();
        if (messages == null || messages.isEmpty()) {
            return List.of(Map.of("role", "user", "content", request.getMessage()));
        }
        return messages;
    }
}
