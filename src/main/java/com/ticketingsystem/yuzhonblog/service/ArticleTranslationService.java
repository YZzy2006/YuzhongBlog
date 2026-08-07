package com.ticketingsystem.yuzhonblog.service;

import com.ticketingsystem.yuzhonblog.entity.Article;
import com.ticketingsystem.yuzhonblog.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleTranslationService {

    private final AiService aiService;
    private final ArticleRepository articleRepository;
    private final TransactionTemplate transactionTemplate;

    private static final String SYSTEM_PROMPT =
            "你是一位专业的技术文档翻译专家。将以下中文内容翻译为英文。\n" +
            "规则：\n" +
            "- 保留所有代码块不翻译\n" +
            "- 技术术语保留英文原文（如 Spring Boot, JWT, API, Redis, Docker）\n" +
            "- 保持 markdown 格式\n" +
            "- 只输出翻译结果，不要任何解释或前缀";

    private static final int MAX_CONTENT_LENGTH = 6000;

    @Async("translationExecutor")
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        translateAllPending();
    }

    /**
     * Batch translate all published articles missing English content.
     * Skips articles that already have English fields filled.
     */
    public int translateAllPending() {
        List<Long> needTranslation = articleRepository.findIdsNeedingTranslation(1);
        if (needTranslation.isEmpty()) return 0;
        log.info("Found {} published articles needing translation, starting...", needTranslation.size());
        for (Long articleId : needTranslation) {
            doTranslate(articleId);
        }
        return needTranslation.size();
    }

    @Async("translationExecutor")
    public void translateArticle(Long articleId) {
        doTranslate(articleId);
    }

    void doTranslate(Long articleId) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                Article article = articleRepository.findById(articleId).orElse(null);
                if (article == null) return;

                boolean updated = false;

                if (isBlank(article.getTitleEn()) && isNotBlank(article.getTitle())) {
                    String en = translate(article.getTitle());
                    if (isNotBlank(en)) {
                        article.setTitleEn(en);
                        updated = true;
                        log.info("Translated title for article {}: {}", articleId, en);
                    }
                }

                if (isBlank(article.getSummaryEn()) && isNotBlank(article.getSummary())) {
                    String en = translate(article.getSummary());
                    if (isNotBlank(en)) {
                        article.setSummaryEn(en);
                        updated = true;
                        log.info("Translated summary for article {}", articleId);
                    }
                }

                if (isBlank(article.getContentMdEn()) && isNotBlank(article.getContentMd())) {
                    String content = article.getContentMd();
                    if (content.length() > MAX_CONTENT_LENGTH) {
                        content = content.substring(0, MAX_CONTENT_LENGTH);
                    }
                    String en = translate(content);
                    if (isNotBlank(en)) {
                        article.setContentMdEn(en);
                        updated = true;
                        log.info("Translated content for article {}", articleId);
                    }
                }

                if (updated) {
                    articleRepository.save(article);
                    log.info("Auto-translation completed for article {}", articleId);
                }
            });
        } catch (Exception e) {
            log.warn("Auto-translation failed for article {}: {}", articleId, e.getMessage());
        }
    }

    private String translate(String text) {
        if (!aiService.isConfigured()) {
            log.warn("AI not configured, skipping translation");
            return null;
        }
        try {
            List<Map<String, String>> messages = List.of(
                    Map.of("role", "user", "content", "请翻译以下内容为英文：\n\n" + text)
            );
            return aiService.chat(messages, SYSTEM_PROMPT);
        } catch (Exception e) {
            log.warn("Translation call failed: {}", e.getMessage());
            return null;
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private boolean isNotBlank(String s) {
        return !isBlank(s);
    }
}
