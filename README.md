<div align="center">

# 🐼 雨中的研发日志 · YuzhonBlog

**个人博客与内容创作平台：文章创作 · 作品集展示 · 动态时间线 · 相册图库 · 音乐电台 · AI 智能助手 · 多语言与主题切换 · 分享卡片**

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen)
![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Data JPA](https://img.shields.io/badge/JPA-Hibernate-59666C)
![Vue](https://img.shields.io/badge/Vue%203-4FC08D)
![Vite](https://img.shields.io/badge/Vite-6.4-646CFF)
![Element Plus](https://img.shields.io/badge/Element%20Plus-2.14-409EFF)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![JWT](https://img.shields.io/badge/Auth-JWT-black)
![AI](https://img.shields.io/badge/AI-OpenAI%20Compatible-purple)
![ECharts](https://img.shields.io/badge/ECharts-6-AA344D)
![Pinia](https://img.shields.io/badge/Pinia-3-FFD859)
![Spring Security](https://img.shields.io/badge/Spring%20Security-7.1-green)
![CI](https://github.com/YZzy2006/YuzhongBlog/actions/workflows/ci.yml/badge.svg)

**从文章的发布分享，到作品集的展示、动态的记录、相册与音乐的收藏，再到 AI 助手的智能问答与写作——一个前后端分离、功能完整的现代化个人博客。**

🔗 在线演示：[https://shunjumc.cn](https://shunjumc.cn) · 📦 开源仓库：[GitHub](https://github.com/YZzy2006/YuzhongBlog)

</div>

---

## ✨ 功能亮点

| | | |
|---|---|---|
| ✍️ **文章创作** | 🎨 **作品集展示** | 📅 **动态时间线** |
| Markdown 编辑器 · 分类标签 · 多语言 · SEO 分享卡片 | 项目卡片 · 封面图 · 技术栈 · 截图画廊 | 短记录 · 心情 · 图片 · 时间轴 |
| 📷 **相册图库** | 🎵 **音乐电台** | 🤖 **AI 智能助手** |
| 云相册 · OSS 存储 · 相册管理 | Bilibili 集成 · 歌词滚动 · 封面 | 多提供商 SSE 流式 · 写作/翻译/分析 |
| 🌙 **主题与多语言** | 🛡️ **安全体系** | 🌌 **互动体验** |
| 自动暗色 · i18n 中英 | JWT · 限流 · 敏感词 · AES 加密 · 内容审核 | 星空宇宙 · 宠物 · 在线游戏 |

---

## 📑 目录

- [一、项目背景](#一项目背景)
- [二、系统架构](#二系统架构)
- [三、核心功能模块](#三核心功能模块)
- [四、关键技术点与难点解决](#四关键技术点与难点解决)
- [五、数据模型设计](#五数据模型设计24-个实体)
- [六、工程结构一览](#六工程结构一览)
- [七、快速运行指南](#七快速运行指南)
- [八、环境变量配置](#八环境变量配置)
- [九、常见问题 FAQ](#九常见问题-faq)
- [十、项目亮点总结](#十项目亮点总结)
- [十一、项目收获与反思](#十一项目收获与反思)
- [十二、界面预览](#十二界面预览)

---

## 一、项目背景

写博客不只是记录，更是一个持续打磨技术的过程。从最初的单页展示，逐步迭代成**集内容创作、作品集、动态、相册、音乐、AI 于一体**的完整平台。

本项目以"一个人也要做好内容平台"为理念，围绕**创作 → 展示 → 互动 → 沉淀**的闭环展开：

- ✍️ 写文章、发动态、收录作品集 → 内容沉淀
- 🎨 相册、音乐、主题、多语言 → 体验打磨
- 🤖 AI 助手、智能搜索、语义问答 → 智能增强
- 🔗 分享卡片、社交链接、留言互动 → 对外传播

---

## 二、系统架构

### 1. 技术选型总览

| 层面 | 技术 | 选型理由 |
|------|------|----------|
| 语言 | Java 17 | 稳定、生态成熟，企业级后端标准 |
| 框架 | Spring Boot 4.1.0 | 自动装配 + 约定优于配置，快速迭代 |
| 持久层 | Spring Data JPA + Hibernate 7 | 实体驱动建模，复杂查询可写 JPQL |
| 数据库 | MySQL 8.0 | 关系型数据模型，保障内容一致性 |
| 认证 | JWT (jjwt 0.12.6) | 无状态令牌，4 条安全过滤器链 + Token 刷新 |
| 密码加密 | BCrypt | 加盐哈希，抗彩虹表攻击 |
| 字段加密 | AES-256-GCM | 敏感配置（OSS / AI 密钥）加密存储 |
| AI 大模型 | OpenAI / Anthropic 兼容协议 | WebClient 异步 + SSE 流式，多模型可配置 |
| 对象存储 | 阿里云 OSS | 文章封面 / 相册 / 头像云端存储 |
| 前端 | Vue 3 + Vite + Element Plus + Pinia | 组件化 + 状态管理 |
| 可视化 | ECharts 6 + md-editor-v3 + three.js | 数据看板 + Markdown + 星空交互 |
| 安全 | Spring Security + sensitive-word | 认证授权 + 敏感词过滤 + 内容审核 |

### 2. 系统架构图

```mermaid
graph TB
    subgraph 客户端
        B[浏览器 / 微信QQ爬虫]
        W[Web 管理端<br/>Vue 3 + Element Plus]
    end
    subgraph Nginx["Nginx 静态资源 + 反代"]
        N[前端静态资源 / SPA 路由回退]
        A["/api 与 /admin 请求反代到后端"]
    end
    subgraph Backend["后端 Spring Boot :8080"]
        C[Controller 层 · 38个 / 169个映射方法]
        S[Service 层 · 业务与事务]
        R[Repository 层 · Spring Data JPA]
        E[Entity 层 · 24实体]
        F[SpaNavigationFilter 注入 OG 分享卡片]
        X[Security 4条FilterChain / 限流 / 敏感词 / 全局异常]
    end
    subgraph 数据与外部服务
        DB[(MySQL 8.0 yuzhong_blog)]
        OSS[阿里云 OSS]
        AI[OpenAI/Anthropic 兼容大模型]
    end

    B -->|HTTPS| N
    W -->|API 请求| A
    A --> C
    N -->|SPA 页面反代| F
    C --> S --> R --> E --> DB
    X -.横切.-> C
    F -.注入 og 标签.-> B
    C -.上传.-> OSS
    S -.WebClient·SSE 流式.-> AI
```

### 3. 分层架构

经典**四层架构**，职责清晰、易于测试与扩展：

```
Controller（接口层） → Service（业务层） → Repository（数据访问） → Entity → MySQL
                          ↕
   Security 过滤器链 / 限流 / 敏感词过滤 / 内容审核 / 全局异常 / OG 注入
```

- **Controller 层**：38 个 REST 控制器（公开端 + 管理端）、169 个 HTTP 映射方法，普通 JSON 接口使用 `ApiResponse`，另有 SSE 流、文件与媒体响应
- **Service 层**：核心业务逻辑与事务边界（`@Transactional`），36 个 Service 类
- **Repository 层**：Spring Data JPA，24 个 Repository，复杂查询用 JPQL + `@EntityGraph`
- **横切关注点**：JWT 无状态认证、登录 / AI / 通用三级限流、敏感词过滤、AI 内容审核、AES-256-GCM 加解密、文件上传 magic-byte 校验、SSRF 防护、全局异常处理

### 4. 前后端分离部署

前端 Vue 3 构建产物输出到 `src/main/resources/static/`，**不打包进后端 JAR**。生产环境由 **nginx 直接提供静态资源**，SPA 页面请求反代到后端注入分享卡片 OG 标签；后端通过 `app.spa.index-path` 读取独立部署的 `index.html`，JAR 监听 **8080**。开发模式下前端运行于 **5173**，通过 Vite 代理 `/api` 与 `/admin` API 请求到 8080。

---

## 三、核心功能模块

### 1️⃣ 文章创作系统

- Markdown 编辑器（md-editor-v3）+ 实时预览 + 图片上传
- 分类 / 标签 / 置顶 / 发布时间管理，标题 / 摘要 / Markdown 正文关键词检索
- **SEO 分享卡片**：文章链接分享到微信 / QQ 自动生成标题 + 摘要 + 封面
- 多语言内容（中 / 英标题与摘要自适应）

### 2️⃣ 作品集展示

- 项目卡片（封面 / 副标题 / 技术栈 / 链接），精选项目置顶展示
- 项目详情：Markdown 介绍 + 功能特性 + **截图画廊灯箱**
- 分享卡片：项目链接分享自动带项目名 / 简介 / 封面

### 3️⃣ 动态与时间线

- 短动态记录：文字 + 图片 + 心情 + 分类
- 时间轴归档（Archive），按月检索
- 点赞、浏览计数，动态详情页

### 4️⃣ 相册图库

- 云相册：封面 + 多图，OSS 云端存储
- 相册管理（后台）、图片灯箱浏览、懒加载

### 5️⃣ 音乐电台

- **Bilibili 集成**：视频号点歌，音乐列表 + 歌词滚动
- 封面自动裁切、自定义歌曲封面覆盖、本地歌单管理

### 6️⃣ AI 智能助手

| 方向 | 功能 |
|------|------|
| 对话助手 | 全站 AI 对话（SSE 流式），多轮上下文记忆 |
| 创作辅助 | 文章 / 项目 / 动态写作助手：生成、润色、扩写、压缩、翻译、改写 |
| 智能搜索 | 自然语言解析为文章关键词、分类、标签与排序条件，再查询数据库；未配置或解析失败时降级为关键词搜索 |
| 模型管理 | 多提供商配置（API 地址 / 模型 / 密钥），密钥 AES-256-GCM 加密存储，余额查询 |

> 💡 统一通过 `WebClient` 调用 OpenAI / Anthropic 兼容协议大模型，`Flux` 以 SSE 流式输出；可接入提供兼容接口的模型服务，管理后台可视化配置。

### 7️⃣ 全局搜索

- 文章关键词检索（标题 / 摘要 / Markdown 正文）+ **AI 智能搜索**（自然语言意图解析与条件筛选）
- 命令面板（Command Palette）按页面名与关键词筛选导航命令

### 8️⃣ 管理后台

- 运营看板（ECharts 仪表盘）：文章 / 访问 / 互动统计
- 内容管理：文章、分类、标签、作品集、动态、相册、公告、友链、留言
- 系统管理：站点设置、AI 配置、OSS 配置、天气配置、用户与角色权限（RBAC）
- 运维能力：SQL 一键备份恢复、登录日志、内容审核记录

### 9️⃣ 系统安全与运维

- **JWT 认证**：4 条无状态安全过滤器链（登录端点 → 超级管理员专属接口 → 其他管理接口 → 默认公开请求），Access Token 刷新，跨标签页会话同步
- **登录限流**：IP 维度窗口计数，登录 / 刷新 / 手机验证码端点配置独立限流逻辑
- **接口限流**：登录 / AI / 通用 API 分级限流
- **内容安全**：敏感词过滤 + **AI 内容审核与人工审批**，具体发布路径按角色处理；AI 未配置或审核调用失败时采用放行策略
- **数据安全**：密码 BCrypt、敏感配置 AES-256-GCM、密钥环境变量化、`.gitignore` 排除本地产物
- **文件安全**：上传 magic-byte 校验、SVG 附件防护、OSS 签名
- **安全响应头**：CSP / X-Frame-Options / X-Content-Type-Options 等；机器人检测

---

## 四、关键技术点与难点解决

### 1. SPA 分享卡片（OG 注入）

微信 / QQ 抓取链接靠的是 HTML `<head>` 里的 `og:` 标签。SPA 页面由 nginx 直接返回静态 `index.html`，爬虫抓不到动态内容。

**解决**：`SpaNavigationFilter` 拦截非 API 页面请求，后端从 `app.spa.index-path` 读取 `index.html`，`OgMetaService` 按路由（文章 / 项目 / 动态）注入 `og:title / og:description / og:image`，并给 OSS 图片追加 `x-oss-process` 压缩参数。nginx 配置 `try_files $uri @backend` 让页面请求反代到后端注入。

### 2. JWT 无状态认证 + 4 条安全过滤器链

`SecurityConfig` 按 `@Order(1..4)` 声明 4 条 `SecurityFilterChain`，请求使用首条匹配的链：**登录端点（限流、放行）→ 超级管理员专属接口（角色校验）→ 其他管理接口（JWT 认证）→ 默认公开请求（API / AI 限流）**。Access Token 默认 2h、Refresh Token 默认 7 天，`JwtAuthenticationFilter` 解析 Bearer Token，实现无状态认证。

### 3. 三级限流防滥用

- **登录接口**：`LoginRateLimiter` 按 IP 维护时间窗口与请求计数
- **AI 接口**：独立限流，防止大模型接口被刷
- **通用 API**：IP 固定时间窗口计数 + 请求触发的过期记录清理；限流状态保存在单个应用实例内存中

### 4. AI 多提供商集成

- 流式对话使用 `WebClient` 订阅响应 + `Flux` SSE 输出，按数据块返回内容；普通对话 / 搜索解析使用阻塞等待结果的调用路径
- 连接测试与内容审核设置 30s 超时，流式响应设置 120s 超时，异常通过各自的错误处理或降级逻辑返回
- 多提供商抽象（OpenAI / Anthropic 兼容），密钥 AES-256-GCM 加密落库，管理后台可视化配置与连通性测试

### 5. 安全设计

- ✅ BCrypt 加盐加密存储密码，不存明文
- ✅ **数据库密码、JWT 密钥、AES 密钥通过环境变量注入；OSS 支持环境变量兜底，OSS / AI 也可在后台配置**
- ✅ 后台保存的 OSS AccessKeySecret / AI API Key 使用 AES-GCM 加密入库；AES 密钥使用 Base64 编码的 32 字节随机值
- ✅ 按发布路径与角色执行 AI 审核 / 人工审批，配合敏感词过滤
- ✅ 文件上传 magic-byte 校验，防 Content-Type 伪装
- ✅ IP 窗口计数限流逻辑；SSRF 防护；安全响应头
- ✅ `.gitignore` 排除本地配置、数据库备份与构建产物，初始化 SQL 保留在仓库内

---

## 五、数据模型设计（24 个实体）

| 域 | 表 / 实体 | 职责 |
|----|-----------|------|
| 内容域 (12) | `article` / `category` / `tag` / `article_tag` / `article_like` / `project` / `timeline_entry` / `timeline_like` / `announcement` / `friend_link` / `content_review` / `report` | 文章、分类标签、作品集、动态、公告、友链、内容审核、举报 |
| 媒体与音乐 (4) | `photo` / `photo_album` / `music_custom_song` / `song_cover_override` | 相册照片、音乐歌单、封面覆盖 |
| 用户与权限 (3) | `admin_user` / `user_permission` / `phone_binding` | 管理员、RBAC 权限、手机绑定 |
| 系统与配置 (5) | `site_setting` / `ai_config` / `weather_config` / `login_log` / `backup_record` | 站点 KV 配置、AI 模型、天气、登录日志、备份 |

**核心数据关系（含业务 ID 关联）：**

```
Category 1──N Article N──M Tag（article_tag）
Article 1──N ArticleLike        TimelineEntry 1──N TimelineLike
PhotoAlbum 1──N Photo（album_id） AdminUser 1──N UserPermission（user_id）
Article / Project / Announcement 1──N ContentReview（content_type + content_id）
SiteSetting：每行保存一个唯一 setting_key 及对应 setting_value
```

> 📐 24 个 JPA 实体包含显式关联实体 `ArticleTag`；相册照片、用户权限、内容审核等通过业务 ID 关联，不全部声明为 JPA 关系。

---

## 六、工程结构一览

```
YuzhonBlog
├── src/main/java/com/ticketingsystem/yuzhonblog/
│   ├── controller/      # 38 个 REST 控制器（公开端 + admin/ 管理端）
│   ├── service/         # 36 个 Service 类（业务逻辑与事务）
│   ├── repository/      # 24 个 JPA Repository + ArticleRepository 自定义查询接口与实现
│   ├── entity/          # 24 个 JPA 实体（继承 BaseEntity）
│   ├── dto/             # 请求 / 响应 DTO
│   ├── common/          # ApiResponse / ErrorCode / BusinessException / BaseEntity
│   ├── config/          # Security / SpaNavigation / Web / 数据初始化
│   ├── security/        # JwtAuthenticationFilter / LoginRateLimiter / 限流
│   ├── util/            # JwtUtil / AesUtil
│   └── YuzhonBlogApplication.java
├── src/main/resources/
│   ├── application*.properties   # dev / prod 环境配置
│   ├── db/schema.sql             # 基础表与 admin 占位密码种子，需手动导入
│   └── static/                   # 前端构建产物（部署生成，不入库）
├── frontend/            # Vue 3 SPA（17 公开视图 + 22 管理端视图）
│   ├── src/views/       # 公开端视图 + views/admin/ 管理端
│   ├── src/components/  # 组件库（AI 助手、音乐播放器、星空等）
│   ├── src/stores/      # Pinia 状态（auth 等）
│   └── public/pets/     # 交互宠物素材（仅默认熊猫入库）
├── docs/                # 文档与界面截图
├── .github/workflows/   # CI 流水线
└── .ai-memory/          # 项目知识库（架构 / 数据模型 / API / 已知问题）
```

---

## 七、快速运行指南

### 环境要求

JDK 17+ · Maven 3.6.3+ · MySQL 8.0+ · Node.js 22.12+（前端开发与构建，满足锁文件中的依赖要求）

### 1. 获取代码并初始化数据库

以下终端示例使用 Bash / Git Bash，在仓库根目录执行；Windows PowerShell 请使用 `$env:变量名 = '值'` 设置环境变量。

```bash
git clone https://github.com/YZzy2006/YuzhongBlog.git
cd YuzhongBlog

mysql -u root -p < src/main/resources/db/schema.sql
```

> ⚠️ `schema.sql` 创建默认数据库、基础表及带占位密码的 `admin` 记录。`dev` / `prod` 均设置 `spring.sql.init.mode=never`，不会自动导入脚本；开发环境的 JPA `ddl-auto=update` 在启动时补齐其余实体表，`DataInitializer` 只替换已有管理员记录的占位密码，不创建账号。请仅对新库执行初始化脚本。

### 2. 配置环境变量

数据库、JWT 密钥、AES 密钥等通过环境变量注入（详见[第八节](#八环境变量配置)）。

```bash
export DB_PASSWORD='替换为本机 MySQL 密码'
export JWT_SECRET="$(openssl rand -hex 32)"
export APP_ENCRYPTION_SECRET="$(openssl rand -base64 32)"
export APP_ADMIN_INITPASSWORD='替换为首次登录的管理员密码'
```

> 🔑 `APP_ADMIN_INITPASSWORD` 对应代码中的 `app.admin.init-password`；原先说明中的 `ADMIN_INIT_PASSWORD` 未在配置文件中映射，单独设置不会生效。生成的 JWT / AES 密钥需妥善保存并在重启时复用，尤其不要更换用于解密已入库配置的 AES 密钥。上述随机密钥生成命令需要 OpenSSL。

### 3. 启动后端

```bash
mvn spring-boot:run          # http://localhost:8080
```

### 4. 启动前端开发模式

另开终端，在仓库目录执行：

```bash
cd frontend
npm ci
npm run dev                  # http://localhost:5173（Vite 代理 /api 到 8080）
```

浏览器访问 **http://localhost:5173**，管理端为 **http://localhost:5173/admin/login**，初始用户名为 `admin`。仅启动后端提供 API，不会自动生成前端页面。

### 5. 生产部署（前后端分离）

```bash
cd frontend
npm ci
npm run build                          # 构建前端 → ../src/main/resources/static/
cd ..
mvn package -DskipTests                 # 打包后端 JAR（不包含 static/）
```

- 前端产物 `src/main/resources/static/` → nginx 站点目录；`app.spa.index-path` 必须指向后端进程可读取的同一份 `index.html`，可通过 `APP_SPA_INDEXPATH` 覆盖仓库中的示例路径
- 配置 `SPRING_PROFILES_ACTIVE=prod`、数据库账号密码、JWT / AES 密钥及 `CORS_ORIGINS`。生产使用 `ddl-auto=validate`，需先准备与全部实体一致的表结构；新库可先完成上述开发初始化流程，单独导入基础 `schema.sql` 不足以通过生产校验
- 后端产物 `target/YuzhonBlog-0.0.1-SNAPSHOT.jar` → 服务器，重命名为 `YuzhonBlog.jar`，在 JAR 与 `start-prod.sh` 所在目录运行 `bash start-prod.sh`，监听 8080
- **nginx 需同时转发 `/api/`、`/admin/` 与 SPA 页面请求**；静态资源由 nginx 提供。以下片段放入已设置 `root` 的站点 `server` 中，根路径也经过后端以注入 OG 标签：

```nginx
location = / {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}

location /api/ {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_buffering off;               # SSE 流式输出
    proxy_read_timeout 180s;
}

location /admin/ {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}

location / {
    try_files $uri @backend;
}

location @backend {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}
```

---

## 八、环境变量配置

| 变量 | 必填 | 默认值 | 说明 |
|------|:---:|--------|------|
| `SPRING_PROFILES_ACTIVE` | ❌ | `dev` | 激活的配置文件（dev / prod） |
| `JWT_SECRET` | ✅ | — | JWT 签名密钥（≥32 字节） |
| `APP_ENCRYPTION_SECRET` | ✅ | — | AES-256-GCM 密钥，Base64 编码 32 字节，用于敏感字段加密 |
| `APP_ADMIN_INITPASSWORD` | ✅(新库) | — | 对应 `app.admin.init-password`，替换已导入 admin 记录的占位密码 |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | ❌ | localhost / 3306 / yuzhong_blog | 生产数据库连接 |
| `DB_USERNAME` | ❌(prod) | root | 生产数据库用户名；开发环境默认也是 root，可通过 `SPRING_DATASOURCE_USERNAME` 覆盖 |
| `DB_PASSWORD` | ✅ | — | 开发 / 生产数据库密码 |
| `SPRING_DATASOURCE_URL` | ❌ | 按 profile 配置 | 可覆盖完整 JDBC URL；开发环境的 `DB_HOST` / `DB_PORT` / `DB_NAME` 不参与 URL 拼接 |
| `OSS_ACCESS_KEY_ID` | ❌ | — | 阿里云 OSS AccessKeyId |
| `OSS_ACCESS_KEY_SECRET` | ❌ | — | 阿里云 OSS AccessKeySecret |
| `CORS_ORIGINS` | ✅(prod) | https://yourdomain.com | 生产需改为实际站点来源，多个来源用逗号分隔 |
| `APP_SPA_INDEXPATH` | ✅(prod) | /www/wwwroot/shunjumc.cn/index.html | 对应 `app.spa.index-path`，需改为部署后实际的 index.html 绝对路径 |

> 🔒 数据库 / JWT / AES 密钥通过环境变量注入；OSS 可用环境变量兜底，后台保存的 OSS AccessKeySecret / AI API Key 加密入库。自定义属性的环境变量名遵循 [Spring Boot 环境变量绑定规则](https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.typesafe-configuration-properties.relaxed-binding.environment-variables)。

---

## 九、常见问题 FAQ

**Q1：启动报 `jwt.secret must be at least 32 characters`？**
未配置 `JWT_SECRET` 环境变量或长度不足。请设置 ≥32 字节的签名密钥。

**Q2：启动报 `app.encryption.secret` 相关错误？**
检查 `APP_ENCRYPTION_SECRET` 是否已设置，且 Base64 解码后为 32 字节。生成示例见第七节；已有加密配置时须使用原密钥。

**Q3：没有管理员账号？**
先对新库手动导入 `src/main/resources/db/schema.sql`，再设置 `APP_ADMIN_INITPASSWORD` 并启动。`DataInitializer` 只更新占位密码，不插入缺失的账号；`ADMIN_INIT_PASSWORD` 单独设置不会生效。

**Q4：AI 功能不可用？**
需在管理后台「AI 设置」中配置模型提供商（API 地址 / 模型 / 密钥）。不配置不影响其余功能。

**Q5：链接分享到微信 / QQ 没有卡片？**
确认 nginx 已配置 SPA 页面反代到后端，根路径 `/` 也需经过后端，并检查 `APP_SPA_INDEXPATH` 指向可读取的 `index.html`。可查看页面源代码确认路由对应的 OG 标签已注入；平台缓存也可能影响展示。

**Q6：前端页面 404 / 白屏？**
生产请确认 nginx 站点目录、静态资源与 `APP_SPA_INDEXPATH` 正确；JAR 不包含前端构建产物。开发模式需先执行 `npm ci`、`npm run dev`，使用 5173 端口并保持后端运行。

---

## 十、项目亮点总结

| 维度 | 亮点 |
|------|------|
| 📝 内容完整度 | 文章 / 作品集 / 动态 / 相册 / 音乐 / 公告，创作到展示全链路 |
| 🤖 AI 深度集成 | 多提供商 SSE 流式 + 创作助手 + 自然语言搜索条件解析 |
| 🔗 社交传播 | 文章 / 项目 / 动态链接一键分享卡片，微信 / QQ 自动渲染 |
| 🌙 体验打磨 | 自动暗色主题、中英多语言、星空宇宙互动、交互宠物 |
| 🛡️ 安全性 | JWT + 三级限流 + BCrypt + AES-256-GCM + 敏感词 + AI 内容审核 + 文件校验 |
| 🔐 运维能力 | SQL 备份恢复 + 登录日志 + 内容审核记录 + CI 流水线 |
| 📐 工程规范性 | 四层架构、JSON 接口统一返回、全局异常、`.gitignore` 排除本地产物 |

---

## 十一、项目收获与反思

> 这个项目让我将 **Spring Boot 分层架构、Spring Data JPA 建模、JWT 认证、AI 多提供商流式集成、对象存储（OSS）、前后端分离部署、分享卡片 OG 注入** 完整串联成一条线。

也让我深刻认识到：

> **一个"能用"的系统，与一个"安全、规范、可维护"的系统之间，差的正是那些看不见的细节——密钥环境变量化、敏感词与内容审核、`.gitignore` 安全排除、部署反代链路。这些经验，是教程上学不到的。**

---

## 🙏 致谢

- **前端框架**：[Vue 3](https://vuejs.org/) · [Element Plus](https://element-plus.org/) · [Pinia](https://pinia.vuejs.org/) · [Vite](https://vitejs.dev/) · [ECharts](https://echarts.apache.org/)
- **后端框架**：[Spring Boot](https://spring.io/projects/spring-boot) · [Spring Data JPA](https://spring.io/projects/spring-data-jpa) · [Spring Security](https://spring.io/projects/spring-security)
- **编辑器与渲染**：[md-editor-v3](https://github.com/imzbf/md-editor-v3) · [marked](https://marked.js.org/) · [KaTeX](https://katex.org/)
- **基础设施**：MySQL · 阿里云 OSS · 微信 / QQ 分享卡片

> 📌 仓库保留初始化 SQL 与界面截图；运行所需密钥通过环境变量或后台配置提供。

---

## 十二、界面预览

<details open>
<summary>👆 点击展开 · 界面截图</summary>

<div align="center">

**展示图 1**

![展示图 1](docs/screenshots/01.png)

**展示图 2**

![展示图 2](docs/screenshots/02.png)

**展示图 3**

![展示图 3](docs/screenshots/03.png)

**展示图 4**

![展示图 4](docs/screenshots/04.png)

**展示图 5**

![展示图 5](docs/screenshots/05.png)

**展示图 6**

![展示图 6](docs/screenshots/06.png)

**展示图 7**

![展示图 7](docs/screenshots/07.png)

**展示图 8**

![展示图 8](docs/screenshots/08.png)

**展示图 9**

![展示图 9](docs/screenshots/09.png)

**展示图 10**

![展示图 10](docs/screenshots/10.png)

</div>

</details>
