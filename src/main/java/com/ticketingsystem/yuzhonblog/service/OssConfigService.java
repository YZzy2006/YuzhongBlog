package com.ticketingsystem.yuzhonblog.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.ticketingsystem.yuzhonblog.dto.admin.OssConfigRequest;
import com.ticketingsystem.yuzhonblog.entity.SiteSetting;
import com.ticketingsystem.yuzhonblog.repository.SiteSettingRepository;
import com.ticketingsystem.yuzhonblog.util.AesUtil;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OssConfigService {

    private final SiteSettingRepository siteSettingRepository;
    private final AesUtil aesUtil;

    @Value("${aliyun.oss.endpoint:}")
    private String fallbackEndpoint;

    @Value("${aliyun.oss.bucket-name:}")
    private String fallbackBucketName;

    @Value("${aliyun.oss.access-key-id:}")
    private String fallbackAccessKeyId;

    @Value("${aliyun.oss.access-key-secret:}")
    private String fallbackAccessKeySecret;

    private volatile OSS cachedClient;
    private volatile String cachedEndpoint;
    private volatile String cachedAccessKeyId;
    private volatile String cachedAccessKeySecret;

    @PreDestroy
    public void destroy() {
        if (cachedClient != null) {
            cachedClient.shutdown();
        }
    }

    // ==================== Config CRUD ====================

    public Map<String, String> getConfig() {
        Map<String, String> result = new HashMap<>();
        result.put("endpoint", getSetting("oss_endpoint", fallbackEndpoint));
        result.put("bucketName", getSetting("oss_bucket_name", fallbackBucketName));
        result.put("accessKeyId", getSetting("oss_access_key_id", fallbackAccessKeyId));

        String encryptedSecret = getSettingRaw("oss_access_key_secret");
        String secret;
        if (encryptedSecret != null && !encryptedSecret.isBlank()) {
            secret = decryptSecret(encryptedSecret);
        } else {
            secret = fallbackAccessKeySecret;
        }
        result.put("accessKeySecret", maskSecret(secret));

        result.put("customDomain", getSetting("oss_custom_domain", ""));
        return result;
    }

    @org.springframework.cache.annotation.CacheEvict(cacheNames = {"siteInfo", "siteSettings"}, allEntries = true)
    public void saveConfig(OssConfigRequest config) {
        saveSetting("oss_endpoint", config.getEndpoint());
        saveSetting("oss_bucket_name", config.getBucketName());
        saveSetting("oss_access_key_id", config.getAccessKeyId());

        String secret = config.getAccessKeySecret();
        if (secret != null && !secret.isBlank() && !secret.startsWith("****")) {
            saveSetting("oss_access_key_secret", aesUtil.encrypt(secret));
        }

        saveSetting("oss_custom_domain", config.getCustomDomain());

        // Reset cached client so it gets recreated with new config
        synchronized (this) {
            if (cachedClient != null) {
                cachedClient.shutdown();
                cachedClient = null;
            }
        }
        log.info("OSS配置已更新");
    }

    // ==================== OSS Client ====================

    public OSS getOssClient() {
        if (cachedClient == null) {
            synchronized (this) {
                if (cachedClient == null) {
                    String endpoint = getEndpoint();
                    String keyId = getAccessKeyId();
                    String keySecret = getAccessKeySecret();

                    if (endpoint.isBlank() || keyId.isBlank() || keySecret.isBlank()) {
                        log.warn("OSS配置不完整，无法创建客户端");
                        return null;
                    }

                    cachedClient = new OSSClientBuilder().build(endpoint, keyId, keySecret);
                    cachedEndpoint = endpoint;
                    cachedAccessKeyId = keyId;
                    cachedAccessKeySecret = keySecret;
                    log.info("OSS客户端已创建: endpoint={}", endpoint);
                }
            }
        }
        return cachedClient;
    }

    public String getBucketName() {
        String dbValue = getSettingRaw("oss_bucket_name");
        if (dbValue != null && !dbValue.isBlank()) return dbValue;
        return fallbackBucketName;
    }

    public String getEndpoint() {
        String dbValue = getSettingRaw("oss_endpoint");
        if (dbValue != null && !dbValue.isBlank()) return dbValue;
        return fallbackEndpoint;
    }

    public String getAccessKeyId() {
        String dbValue = getSettingRaw("oss_access_key_id");
        if (dbValue != null && !dbValue.isBlank()) return dbValue;
        return fallbackAccessKeyId;
    }

    public String getAccessKeySecret() {
        String encrypted = getSettingRaw("oss_access_key_secret");
        if (encrypted != null && !encrypted.isBlank()) {
            return decryptSecret(encrypted);
        }
        return fallbackAccessKeySecret;
    }

    public String getCustomDomain() {
        return getSetting("oss_custom_domain", "");
    }

    public boolean isConfigured() {
        String endpoint = getEndpoint();
        String bucket = getBucketName();
        String keyId = getAccessKeyId();
        String keySecret = getAccessKeySecret();
        return !endpoint.isBlank() && !bucket.isBlank() && !keyId.isBlank() && !keySecret.isBlank();
    }

    // ==================== Test Connection ====================

    public Map<String, Object> testConnection() {
        Map<String, Object> result = new HashMap<>();
        try {
            OSS client = getOssClient();
            if (client == null) {
                result.put("success", false);
                result.put("message", "OSS配置不完整，请先配置");
                return result;
            }
            String bucket = getBucketName();
            boolean exists = client.doesBucketExist(bucket);
            result.put("success", exists);
            result.put("bucketName", bucket);
            if (exists) {
                result.put("message", "连接成功，Bucket 存在");
            } else {
                result.put("message", "连接成功，但 Bucket 不存在");
            }
        } catch (Exception e) {
            log.error("OSS连接测试失败: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "连接失败: " + e.getMessage());
        }
        return result;
    }

    // ==================== Helpers ====================

    private String getSetting(String key, String defaultValue) {
        String raw = getSettingRaw(key);
        if (raw == null || raw.isBlank()) return defaultValue;
        return raw;
    }

    private String getSettingRaw(String key) {
        return siteSettingRepository.findBySettingKey(key)
                .map(SiteSetting::getSettingValue)
                .orElse(null);
    }

    private void saveSetting(String key, String value) {
        if (value == null) return;
        SiteSetting setting = siteSettingRepository.findBySettingKey(key)
                .orElseGet(() -> {
                    SiteSetting s = new SiteSetting();
                    s.setSettingKey(key);
                    return s;
                });
        setting.setSettingValue(value);
        siteSettingRepository.save(setting);
    }

    private String decryptSecret(String encrypted) {
        try {
            return aesUtil.decrypt(encrypted);
        } catch (Exception e) {
            log.warn("OSS密钥解密失败: {}", e.getMessage());
            return "";
        }
    }

    private static String maskSecret(String secret) {
        if (secret == null || secret.length() <= 4) return "****";
        return "****" + secret.substring(secret.length() - 4);
    }
}
