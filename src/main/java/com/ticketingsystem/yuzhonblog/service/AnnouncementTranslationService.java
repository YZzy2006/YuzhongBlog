package com.ticketingsystem.yuzhonblog.service;

import com.ticketingsystem.yuzhonblog.entity.Announcement;
import com.ticketingsystem.yuzhonblog.repository.AnnouncementRepository;
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
public class AnnouncementTranslationService {

    private final AiService aiService;
    private final AnnouncementRepository announcementRepository;
    private final TransactionTemplate transactionTemplate;

    private static final String SYSTEM_PROMPT =
            "你是一位专业的技术文档翻译专家。将以下中文内容翻译为英文。\n" +
            "规则：\n" +
            "- 技术术语保留英文原文\n" +
            "- 只输出翻译结果，不要任何解释或前缀";

    @Async("translationExecutor")
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        translateAllPending();
    }

    public int translateAllPending() {
        List<Announcement> needTranslation = announcementRepository.findNeedingTranslation();
        if (needTranslation.isEmpty()) return 0;
        log.info("Found {} announcements needing translation, starting...", needTranslation.size());
        for (Announcement announcement : needTranslation) {
            doTranslate(announcement.getId());
        }
        return needTranslation.size();
    }

    @Async("translationExecutor")
    public void translateAnnouncement(Long announcementId) {
        doTranslate(announcementId);
    }

    void doTranslate(Long announcementId) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                Announcement a = announcementRepository.findById(announcementId).orElse(null);
                if (a == null) return;

                boolean updated = false;

                if (isBlank(a.getTitleEn()) && isNotBlank(a.getTitle())) {
                    String en = translate(a.getTitle());
                    if (isNotBlank(en)) {
                        a.setTitleEn(en);
                        updated = true;
                        log.info("Translated announcement title {}: {}", announcementId, en);
                    }
                }

                if (isBlank(a.getTagEn()) && isNotBlank(a.getTag())) {
                    String en = translate(a.getTag());
                    if (isNotBlank(en)) {
                        a.setTagEn(en);
                        updated = true;
                    }
                }

                if (isBlank(a.getContentEn()) && isNotBlank(a.getContent())) {
                    String en = translate(a.getContent());
                    if (isNotBlank(en)) {
                        a.setContentEn(en);
                        updated = true;
                    }
                }

                if (updated) {
                    announcementRepository.save(a);
                    log.info("Auto-translation completed for announcement {}", announcementId);
                }
            });
        } catch (Exception e) {
            log.warn("Auto-translation failed for announcement {}: {}", announcementId, e.getMessage());
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

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
    private boolean isNotBlank(String s) { return !isBlank(s); }
}
