package com.ticketingsystem.yuzhonblog.service;

import com.ticketingsystem.yuzhonblog.common.BusinessException;
import com.ticketingsystem.yuzhonblog.common.ErrorCode;
import com.ticketingsystem.yuzhonblog.dto.site.SiteInfoResponse;
import com.ticketingsystem.yuzhonblog.dto.site.SiteSettingRequest;
import com.ticketingsystem.yuzhonblog.entity.SiteSetting;
import com.ticketingsystem.yuzhonblog.repository.SiteSettingRepository;
import com.ticketingsystem.yuzhonblog.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SiteSettingService {

    private final SiteSettingRepository siteSettingRepository;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final CurrentUserProvider currentUserProvider;

    @Transactional(readOnly = true)
    @Cacheable("siteInfo")
    public SiteInfoResponse getSiteInfo() {
        List<SiteSetting> all = siteSettingRepository.findAll();
        Map<String, String> map = new HashMap<>();
        for (SiteSetting s : all) {
            map.put(s.getSettingKey(), s.getSettingValue());
        }

        SiteInfoResponse resp = new SiteInfoResponse();
        resp.setSiteName(map.getOrDefault("site_name", "雨中的研发日志"));
        resp.setSiteDescription(map.getOrDefault("site_description", ""));
        map.remove("site_name");
        map.remove("site_description");
        map.entrySet().removeIf(e -> isSensitiveKey(e.getKey()));
        resp.setExtraSettings(map);
        return resp;
    }

    private boolean isSensitiveKey(String key) {
        if (key == null) return false;
        String lower = key.toLowerCase();
        if (lower.equals("oss_custom_domain")) return false;
        return lower.contains("password") || lower.contains("secret") || lower.contains("key");
    }

    @Transactional(readOnly = true)
    @Cacheable("siteSettings")
    public Map<String, String> getAllSettings() {
        List<SiteSetting> all = siteSettingRepository.findAll();
        Map<String, String> map = new HashMap<>();
        for (SiteSetting s : all) {
            map.put(s.getSettingKey(), s.getSettingValue());
        }
        return map;
    }

    @Transactional
    @CacheEvict(cacheNames = {"siteInfo", "siteSettings"}, allEntries = true)
    public void setSetting(String key, String value) {
        SiteSetting setting = siteSettingRepository.findBySettingKey(key)
                .orElseGet(() -> {
                    SiteSetting s = new SiteSetting();
                    s.setSettingKey(key);
                    return s;
                });
        setting.setSettingValue(value);
        siteSettingRepository.save(setting);
    }

    @Transactional
    @CacheEvict(cacheNames = {"siteInfo", "siteSettings"}, allEntries = true)
    public void updateSettings(SiteSettingRequest request) {
        if (request.getSettings() == null) return;
        checkSensitiveContent(request.getSettings());
        List<SiteSetting> toSave = new ArrayList<>();
        for (Map.Entry<String, String> entry : request.getSettings().entrySet()) {
            SiteSetting setting = siteSettingRepository.findBySettingKey(entry.getKey())
                    .orElseGet(() -> {
                        SiteSetting s = new SiteSetting();
                        s.setSettingKey(entry.getKey());
                        return s;
                    });
            setting.setSettingValue(entry.getValue());
            toSave.add(setting);
        }
        siteSettingRepository.saveAll(toSave);
    }

    private void checkSensitiveContent(Map<String, String> settings) {
        // 管理员和超级管理员跳过内容检测
        String role = currentUserProvider.getCurrentUser().getRole();
        if ("admin".equals(role) || "super_admin".equals(role)) return;

        String siteName = settings.get("site_name");
        if (siteName != null) {
            String result = sensitiveWordFilter.check(siteName);
            if (result != null) throw new BusinessException(ErrorCode.SENSITIVE_WORD_BLOCKED.getCode(), "站点名称" + result);
        }
        String siteDesc = settings.get("site_description");
        if (siteDesc != null) {
            String result = sensitiveWordFilter.check(siteDesc);
            if (result != null) throw new BusinessException(ErrorCode.SENSITIVE_WORD_BLOCKED.getCode(), "站点描述" + result);
        }
    }
}
