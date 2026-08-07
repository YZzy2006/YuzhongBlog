package com.ticketingsystem.yuzhonblog.config;

import com.ticketingsystem.yuzhonblog.util.AesUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final AesUtil aesUtil;

    @Value("${app.admin.init-password:}")
    private String adminInitPassword;

    @Override
    public void run(String... args) {
        initAdminPassword();
        migrateAdminRole();
        migrateAiSettings();
        createDatabaseViews();
    }

    private void initAdminPassword() {
        if (adminInitPassword == null || adminInitPassword.isBlank()) {
            log.warn("未设置 app.admin.init-password（环境变量 ADMIN_INIT_PASSWORD），跳过默认管理员密码初始化");
            return;
        }
        String placeholder = "$2a$10$PLACEHOLDER_WILL_BE_REPLACED_ON_STARTUP";
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_user WHERE password_hash = ?",
                Integer.class, placeholder);

        if (count != null && count > 0) {
            String encoded = passwordEncoder.encode(adminInitPassword);
            jdbcTemplate.update(
                    "UPDATE admin_user SET password_hash = ? WHERE password_hash = ?",
                    encoded, placeholder);
            log.info("管理员默认密码已初始化，请尽快登录后台修改！");
            log.warn("请尽快登录后台修改默认密码！");
        }
    }

    private void migrateAdminRole() {
        try {
            // 1. 空角色默认设为 super_admin
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM admin_user WHERE role IS NULL OR role = ''",
                    Integer.class);
            if (count != null && count > 0) {
                jdbcTemplate.update("UPDATE admin_user SET role = 'super_admin' WHERE role IS NULL OR role = ''");
                log.info("已为 {} 个现有管理员设置默认角色: super_admin", count);
            }
            // 2. 确保用户名为 admin 的用户是 super_admin
            int updated = jdbcTemplate.update(
                    "UPDATE admin_user SET role = 'super_admin' WHERE username = 'admin' AND role != 'super_admin'");
            if (updated > 0) {
                log.info("已将 admin 用户提升为 super_admin");
            }
            // 3. 确保第一个 admin 用户（id最小的）是 super_admin
            Long minId = jdbcTemplate.queryForObject(
                    "SELECT MIN(id) FROM admin_user", Long.class);
            if (minId != null) {
                int updated2 = jdbcTemplate.update(
                        "UPDATE admin_user SET role = 'super_admin' WHERE id = ? AND role != 'super_admin'",
                        minId);
                if (updated2 > 0) {
                    log.info("已将首个管理员(id={})提升为 super_admin", minId);
                }
            }
        } catch (Exception e) {
            log.warn("角色字段迁移检查失败: {}", e.getMessage());
        }
    }

    private void migrateAiSettings() {
        try {
            // Check if ai_config table exists and is empty
            Integer configCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM ai_config", Integer.class);
            if (configCount == null || configCount > 0) return;

            // Check if old ai_base_url exists in site_setting
            String oldBaseUrl = jdbcTemplate.queryForObject(
                    "SELECT setting_value FROM site_setting WHERE setting_key = 'ai_base_url'",
                    String.class);
            if (oldBaseUrl == null || oldBaseUrl.isBlank()) return;

            String oldApiKey = getSiteSetting("ai_api_key");
            String oldModel = getSiteSetting("ai_model");
            String oldMaxTokens = getSiteSetting("ai_max_tokens");
            String oldTemperature = getSiteSetting("ai_temperature");
            String oldApiFormat = getSiteSetting("ai_api_format");
            String oldAuthType = getSiteSetting("ai_auth_type");
            String oldWebsiteUrl = getSiteSetting("ai_website_url");
            String oldBalanceUrl = getSiteSetting("ai_balance_url");
            String oldBalanceScript = getSiteSetting("ai_balance_script");
            String oldName = getSiteSetting("ai_name");
            String oldDescription = getSiteSetting("ai_description");

            if (oldApiKey == null || oldApiKey.isBlank()) return;

            // Ensure key is encrypted
            String encryptedKey = oldApiKey;
            if (!oldApiKey.startsWith("ENC:")) {
                encryptedKey = aesUtil.encrypt(oldApiKey);
            }

            jdbcTemplate.update(
                    "INSERT INTO ai_config (name, api_key, base_url, model, max_tokens, temperature, " +
                    "api_format, auth_type, website_url, balance_url, balance_script, description, is_active, sort_order) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 0)",
                    oldName != null && !oldName.isBlank() ? oldName : "迁移配置",
                    encryptedKey, oldBaseUrl,
                    oldModel != null && !oldModel.isBlank() ? oldModel : "deepseek-chat",
                    oldMaxTokens != null ? parseInt(oldMaxTokens, 4096) : 4096,
                    oldTemperature != null ? parseDouble(oldTemperature, 0.7) : 0.7,
                    oldApiFormat != null && !oldApiFormat.isBlank() ? oldApiFormat : "OPENAI",
                    oldAuthType != null && !oldAuthType.isBlank() ? oldAuthType : "BEARER",
                    oldWebsiteUrl, oldBalanceUrl, oldBalanceScript, oldDescription);

            log.info("已将旧 AI 配置迁移到 ai_config 表");

            // Clean up old AI keys (keep ai_enabled)
            jdbcTemplate.update("DELETE FROM site_setting WHERE setting_key IN (" +
                    "'ai_name', 'ai_api_key', 'ai_base_url', 'ai_model', 'ai_max_tokens', " +
                    "'ai_temperature', 'ai_api_format', 'ai_auth_type', 'ai_website_url', " +
                    "'ai_balance_url', 'ai_balance_script', 'ai_description')");
            log.info("已清理 site_setting 中的旧 AI 配置键");
        } catch (Exception e) {
            log.debug("AI 配置迁移检查跳过: {}", e.getMessage());
        }
    }

    private String getSiteSetting(String key) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT setting_value FROM site_setting WHERE setting_key = ?",
                    String.class, key);
        } catch (Exception e) {
            return null;
        }
    }

    private int parseInt(String s, int defaultValue) {
        try { return Integer.parseInt(s); } catch (Exception e) { return defaultValue; }
    }

    private double parseDouble(String s, double defaultValue) {
        try { return Double.parseDouble(s); } catch (Exception e) { return defaultValue; }
    }

    private void createDatabaseViews() {
        // Category article count view
        executeView("v_category_article_count",
                "CREATE OR REPLACE VIEW v_category_article_count AS " +
                "SELECT c.id AS category_id, c.name AS category_name, " +
                "COUNT(a.id) AS article_count " +
                "FROM category c LEFT JOIN article a ON a.category_id = c.id AND a.status = 1 " +
                "GROUP BY c.id, c.name");

        // Tag article count view
        executeView("v_tag_article_count",
                "CREATE OR REPLACE VIEW v_tag_article_count AS " +
                "SELECT t.id AS tag_id, t.name AS tag_name, " +
                "COUNT(at2.id) AS article_count " +
                "FROM tag t LEFT JOIN article_tag at2 ON at2.tag_id = t.id " +
                "LEFT JOIN article a ON a.id = at2.article_id AND a.status = 1 " +
                "GROUP BY t.id, t.name");

        // Daily login statistics view
        executeView("v_login_daily_stats",
                "CREATE OR REPLACE VIEW v_login_daily_stats AS " +
                "SELECT user_id, DATE(created_at) AS login_date, " +
                "SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS success_count, " +
                "SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) AS fail_count, " +
                "COUNT(*) AS total_count " +
                "FROM login_log " +
                "GROUP BY user_id, DATE(created_at)");

        // Article summary view (no LONGTEXT fields for faster queries)
        executeView("v_article_summary",
                "CREATE OR REPLACE VIEW v_article_summary AS " +
                "SELECT id, title, title_en, summary, summary_en, cover_image, slug, " +
                "status, view_count, like_count, is_top, is_featured, card_style, " +
                "category_id, created_at, updated_at " +
                "FROM article");
    }

    private void executeView(String viewName, String sql) {
        try {
            jdbcTemplate.execute(sql);
            log.debug("数据库视图 {} 创建/更新成功", viewName);
        } catch (Exception e) {
            log.debug("视图 {} 创建跳过: {}", viewName, e.getMessage());
        }
    }

}
