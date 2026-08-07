-- ============================================================
-- 雨中的研发日志 - 数据库初始化脚本
-- 数据库: MySQL 8
-- 字符集: utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS yuzhong_blog
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE yuzhong_blog;

-- ============================================================
-- 1. 管理员账号表
-- ============================================================
CREATE TABLE IF NOT EXISTS admin_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username    VARCHAR(50)  NOT NULL                COMMENT '用户名',
    password_hash VARCHAR(200) NOT NULL              COMMENT '密码哈希（BCrypt）',
    role        VARCHAR(20)  NOT NULL DEFAULT 'admin' COMMENT '角色：super_admin / admin',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员账号表';

-- ============================================================
-- 2. 分类表
-- ============================================================
CREATE TABLE IF NOT EXISTS category (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name        VARCHAR(50)  NOT NULL                COMMENT '分类名称',
    sort_order  INT          DEFAULT 0               COMMENT '排序权重（越大越靠前）',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分类表';

-- ============================================================
-- 3. 标签表
-- ============================================================
CREATE TABLE IF NOT EXISTS tag (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name        VARCHAR(50)  NOT NULL                COMMENT '标签名称',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签表';

-- ============================================================
-- 4. 文章表
-- ============================================================
CREATE TABLE IF NOT EXISTS article (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    title         VARCHAR(200)  NOT NULL                COMMENT '文章标题',
    content_md    LONGTEXT                               COMMENT 'Markdown 原文',
    content_html  LONGTEXT                               COMMENT '渲染后的 HTML',
    summary       VARCHAR(500)                           COMMENT '文章摘要',
    cover_image   VARCHAR(500)                           COMMENT '封面图片 URL',
    slug          VARCHAR(200)                           COMMENT 'URL 别名（用于 SEO 友好链接）',
    status        TINYINT       DEFAULT 0                COMMENT '状态：0=草稿，1=已发布，2=已归档',
    view_count    INT           DEFAULT 0                COMMENT '浏览次数',
    like_count    INT           DEFAULT 0                COMMENT '点赞次数',
    is_top        TINYINT       DEFAULT 0                COMMENT '是否置顶：0=否，1=是',
    category_id   BIGINT                                 COMMENT '所属分类ID',
    created_at    DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_slug (slug),
    KEY idx_status (status),
    KEY idx_category_id (category_id),
    KEY idx_created_at (created_at),
    CONSTRAINT fk_article_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章表';

-- ============================================================
-- 5. 文章标签关联表
-- ============================================================
CREATE TABLE IF NOT EXISTS article_tag (
    id          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    article_id  BIGINT NOT NULL                COMMENT '文章ID',
    tag_id      BIGINT NOT NULL                COMMENT '标签ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_article_tag (article_id, tag_id),
    KEY idx_tag_id (tag_id),
    CONSTRAINT fk_at_article FOREIGN KEY (article_id) REFERENCES article (id) ON DELETE CASCADE,
    CONSTRAINT fk_at_tag     FOREIGN KEY (tag_id)     REFERENCES tag (id)     ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章标签关联表';

-- ============================================================
-- 6. 作品集项目表
-- ============================================================
CREATE TABLE IF NOT EXISTS project (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name          VARCHAR(100) NOT NULL                COMMENT '项目名称',
    description   TEXT                                  COMMENT '项目描述',
    tech_stack    VARCHAR(500)                          COMMENT '技术栈（逗号分隔）',
    cover_image   VARCHAR(500)                          COMMENT '封面图片 URL',
    github_url    VARCHAR(500)                          COMMENT 'GitHub 仓库地址',
    demo_url      VARCHAR(500)                          COMMENT '在线演示地址',
    sort_order    INT          DEFAULT 0                COMMENT '排序权重（越大越靠前）',
    created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作品集项目表';

-- 文章内容字段修正（JPA @Lob 可能生成 tinytext，强制改回 LONGTEXT）
ALTER TABLE article MODIFY COLUMN content_md LONGTEXT COMMENT 'Markdown 原文';
ALTER TABLE article MODIFY COLUMN content_html LONGTEXT COMMENT '渲染后的 HTML';

-- 项目表扩展字段（v2）
ALTER TABLE project ADD COLUMN IF NOT EXISTS subtitle VARCHAR(200) COMMENT '副标题';
ALTER TABLE project ADD COLUMN IF NOT EXISTS features TEXT COMMENT '功能亮点（Markdown）';
ALTER TABLE project ADD COLUMN IF NOT EXISTS subdomain_url VARCHAR(500) COMMENT '子域名地址';
ALTER TABLE project ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PUBLISHED';
ALTER TABLE project ADD COLUMN IF NOT EXISTS is_featured TINYINT DEFAULT 0 COMMENT '是否置顶推荐';

-- ============================================================
-- 7. 站点公告表
-- ============================================================
CREATE TABLE IF NOT EXISTS announcement (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tag         VARCHAR(20)  NOT NULL                COMMENT '标签名',
    tag_en      VARCHAR(30)                          COMMENT '标签名（英文）',
    type        VARCHAR(20)  NOT NULL                COMMENT '类型: info/feature/update',
    title       VARCHAR(200) NOT NULL                COMMENT '公告标题',
    title_en    VARCHAR(200)                         COMMENT '公告标题（英文）',
    content     TEXT                                  COMMENT '公告内容',
    content_en  TEXT                                  COMMENT '公告内容（英文）',
    sort_order  INT          DEFAULT 0               COMMENT '排序权重',
    level        VARCHAR(20)  NOT NULL DEFAULT 'info'    COMMENT '颜色等级: info/success/warning/error',
    display_style VARCHAR(20) NOT NULL DEFAULT 'banner'  COMMENT '展示样式: banner/alert',
    active      TINYINT(1)   NOT NULL DEFAULT 1      COMMENT '是否启用',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站点公告表';

-- ============================================================
-- 8. 站点配置表
-- ============================================================
CREATE TABLE IF NOT EXISTS site_setting (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    setting_key   VARCHAR(100) NOT NULL                COMMENT '配置键',
    setting_value TEXT                                  COMMENT '配置值',
    updated_at    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_setting_key (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站点配置表';

-- ============================================================
-- 8. AI 配置表
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_config (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name          VARCHAR(100) NOT NULL                COMMENT '配置名称',
    api_key       TEXT         NOT NULL                COMMENT 'API Key（AES加密）',
    base_url      VARCHAR(500) NOT NULL                COMMENT 'API 基础地址',
    model         VARCHAR(100) NOT NULL                COMMENT '模型名称',
    max_tokens    INT          DEFAULT 4096            COMMENT '最大Token数',
    temperature   DOUBLE       DEFAULT 0.7             COMMENT '温度参数',
    api_format    VARCHAR(20)  DEFAULT 'OPENAI'        COMMENT 'API格式：OPENAI / ANTHROPIC',
    auth_type     VARCHAR(20)  DEFAULT 'BEARER'        COMMENT '认证方式：BEARER / X_API_KEY / RAW_TOKEN / API_KEY',
    website_url   VARCHAR(500)                         COMMENT '官网地址',
    balance_url   VARCHAR(500)                         COMMENT '余额查询地址',
    balance_script TEXT                                 COMMENT '余额解析脚本（JavaScript）',
    description   VARCHAR(500)                         COMMENT '备注说明',
    is_active     TINYINT      DEFAULT 0               COMMENT '是否激活：0=否，1=是',
    sort_order    INT          DEFAULT 0               COMMENT '排序权重',
    created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI配置表';

CREATE TABLE IF NOT EXISTS weather_config (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name          VARCHAR(100) NOT NULL                COMMENT '配置名称',
    provider      VARCHAR(50)  NOT NULL                COMMENT '供应商：qweather/openweathermap/seniverse/custom',
    api_key       TEXT         NOT NULL                COMMENT 'API Key（AES加密）',
    base_url      VARCHAR(500) NOT NULL                COMMENT 'API 基础地址',
    api_format    VARCHAR(20)  DEFAULT 'json'          COMMENT '响应格式',
    auth_type     VARCHAR(20)  DEFAULT 'query_param'   COMMENT '认证方式：query_param/header',
    language      VARCHAR(10)  DEFAULT 'zh'            COMMENT '响应语言',
    units         VARCHAR(10)  DEFAULT 'c'             COMMENT '温度单位：c/f',
    location      VARCHAR(100)                         COMMENT '默认位置',
    extra_params  TEXT                                 COMMENT '附加参数（JSON）',
    description   VARCHAR(500)                         COMMENT '备注说明',
    is_active     TINYINT      DEFAULT 0               COMMENT '是否激活：0=否，1=是',
    created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_weather_config_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='天气API配置表';

-- ============================================================
-- 种子数据
-- ============================================================

-- 管理员账号由 DataInitializer 在应用首次启动时初始化
-- 初始密码通过环境变量 ADMIN_INIT_PASSWORD 设置（生产必须设置，部署后请立即修改默认密码）
INSERT INTO admin_user (username, password_hash, role)
VALUES ('admin', '$2a$10$PLACEHOLDER_WILL_BE_REPLACED_ON_STARTUP', 'super_admin')
ON DUPLICATE KEY UPDATE username = username;

-- 默认分类
INSERT INTO category (name, sort_order)
VALUES ('未分类', 0)
ON DUPLICATE KEY UPDATE name = name;

-- 站点配置
INSERT INTO site_setting (setting_key, setting_value) VALUES
    ('site_name',        '雨中的研发日志'),
    ('site_description', '记录软件开发中的每一次探索与实践')
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value);

-- 站点公告（按标题去重，每条独立判断）
INSERT INTO announcement (tag, tag_en, type, title, title_en, content, content_en, sort_order, active)
SELECT '欢迎', 'Welcome', 'info', '欢迎来到雨中的研发日志', 'Welcome to Yuz''s Dev Log',
       '这是一个记录软件开发学习笔记、编程实践与项目作品的个人技术博客。本站使用 Spring Boot + Vue 3 构建，持续迭代中。如有建议或问题，欢迎通过「关于我」页面联系。',
       'A personal tech blog for software development notes, coding practice, and project showcases. Built with Spring Boot + Vue 3, continuously evolving. Feel free to reach out via the About page.',
       4, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM announcement WHERE title = '欢迎来到雨中的研发日志');

INSERT INTO announcement (tag, tag_en, type, title, title_en, content, content_en, sort_order, active)
SELECT '新功能', 'New Feature', 'feature', 'AI 智能助手已上线', 'AI Assistant Now Live',
       '首页新增 AI 智能助手功能，支持自然语言提问。基于大语言模型，可以帮你查找文章、解答技术问题。支持流式输出和思考过程展示，快来试试吧！',
       'AI assistant is now available on the homepage — ask questions in natural language. Powered by LLM, it can find articles and answer tech questions. Supports streaming output and thinking process display.',
       3, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM announcement WHERE title = 'AI 智能助手已上线');

INSERT INTO announcement (tag, tag_en, type, title, title_en, content, content_en, sort_order, active)
SELECT '更新', 'Update', 'update', '作品集页面全新改版', 'Projects Page Redesigned',
       '作品集页面已完成重构，支持封面图上传、富文本编辑、状态管理和置顶推荐功能。后续将支持子域名访问，敬请期待。',
       'The Projects page has been rebuilt with cover image upload, rich text editing, status management, and featured pinning. Subdomain access coming soon.',
       2, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM announcement WHERE title = '作品集页面全新改版');

INSERT INTO announcement (tag, tag_en, type, title, title_en, content, content_en, sort_order, active)
SELECT '技术栈', 'Tech Stack', 'info', '本站技术栈一览', 'Tech Stack Overview',
       '后端：Spring Boot 4.1 + JPA/MySQL + JWT 认证。前端：Vue 3 + Vite + md-editor-v3。部署：JAR 单文件部署，前端构建产物内嵌。',
       'Backend: Spring Boot 4.1 + JPA/MySQL + JWT Auth. Frontend: Vue 3 + Vite + md-editor-v3. Deployment: Single JAR with embedded frontend assets.',
       1, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM announcement WHERE title = '本站技术栈一览');
