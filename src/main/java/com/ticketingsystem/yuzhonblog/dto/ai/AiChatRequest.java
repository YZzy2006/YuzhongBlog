package com.ticketingsystem.yuzhonblog.dto.ai;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AiChatRequest {
    private static final int MAX_MESSAGE_CONTENT_LENGTH = 8000;

    @Size(max = 4000, message = "message长度不能超过4000")
    private String message;

    /** Multi-turn conversation messages */
    @Size(max = 50, message = "messages数量不能超过50条")
    private List<Map<String, String>> messages;

    /** Custom system prompt (editor mode) */
    @Size(max = 8000, message = "systemPrompt长度不能超过8000")
    private String systemPrompt;

    /** Max tokens override (null = use config default) */
    private Integer maxTokens;

    /**
     * Validate that each message content does not exceed the limit.
     * Called from controllers since Bean Validation cannot inspect Map values.
     */
    public void validateMessages() {
        if (messages == null) return;
        for (Map<String, String> msg : messages) {
            String content = msg.get("content");
            if (content != null && content.length() > MAX_MESSAGE_CONTENT_LENGTH) {
                throw new com.ticketingsystem.yuzhonblog.common.BusinessException(
                        com.ticketingsystem.yuzhonblog.common.ErrorCode.BAD_REQUEST.getCode(),
                        "消息内容过长，单条不能超过" + MAX_MESSAGE_CONTENT_LENGTH + "字");
            }
        }
    }
}
