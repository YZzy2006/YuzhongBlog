package com.ticketingsystem.yuzhonblog.service;

import com.ticketingsystem.yuzhonblog.entity.Article;
import com.ticketingsystem.yuzhonblog.entity.Project;
import com.ticketingsystem.yuzhonblog.entity.TimelineEntry;
import com.ticketingsystem.yuzhonblog.repository.ArticleRepository;
import com.ticketingsystem.yuzhonblog.repository.ProjectRepository;
import com.ticketingsystem.yuzhonblog.repository.TimelineEntryRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.Map;

/**
 * 为 SPA 页面动态注入 Open Graph meta 标签，
 * 让微信/QQ/微博抓取链接时能渲染出分享卡片。
 * 支持文章、作品集项目、动态详情页，图片使用内容封面或后台可配置的站点头像。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OgMetaService {

    private static final String DEFAULT_SITE_NAME = "雨中的研发日志";
    private static final int MAX_DESC_LENGTH = 200;

    private final ArticleRepository articleRepository;
    private final ProjectRepository projectRepository;
    private final TimelineEntryRepository timelineEntryRepository;
    private final SiteSettingService siteSettingService;

    /**
     * @param html    原始 index.html
     * @param path    请求路径（不含 query）
     * @param request 用于解析 Accept-Language 与完整 URL
     * @return 注入 OG meta 后的 HTML
     */
    public String inject(String html, String path, HttpServletRequest request) {
        if (html == null) return null;

        Map<String, String> settings = loadSettings();
        String siteName = settings.getOrDefault("site_name", DEFAULT_SITE_NAME);
        String siteDesc = settings.getOrDefault("site_description", "");
        String avatar = settings.getOrDefault("site_avatar", "");
        String customDomain = settings.getOrDefault("oss_custom_domain", "");

        String title = siteName;
        String description = siteDesc;
        String image = avatar;
        String type = "website";

        boolean en = isEnglishRequest(request);

        // 文章路由：/articles/{slug}
        if (path != null && path.startsWith("/articles/") && path.length() > "/articles/".length()) {
            String slug = path.substring("/articles/".length());
            if (!slug.contains("/")) {
                Article article = findPublished(slug);
                if (article != null) {
                    title = en && hasText(article.getTitleEn()) ? article.getTitleEn() : article.getTitle();
                    description = en && hasText(article.getSummaryEn()) ? article.getSummaryEn() : article.getSummary();
                    if (hasText(article.getCoverImage())) image = article.getCoverImage();
                    type = "article";
                }
            }
        } else if (path != null && path.startsWith("/projects/")) {
            Long id = parseId(path, "/projects/");
            if (id != null) {
                Project project = projectRepository.findById(id).orElse(null);
                if (project != null && isPublished(project.getStatus())) {
                    title = project.getName();
                    description = hasText(project.getSubtitle()) ? project.getSubtitle() : stripMarkdown(project.getDescription());
                    if (hasText(project.getCoverImage())) image = project.getCoverImage();
                }
            }
        } else if (path != null && path.startsWith("/moments/")) {
            Long id = parseId(path, "/moments/");
            if (id != null) {
                TimelineEntry entry = timelineEntryRepository.findById(id).orElse(null);
                if (entry != null && isPublished(entry.getStatus())) {
                    title = entry.getTitle();
                    description = stripMarkdown(entry.getDescription());
                    if (hasText(entry.getCoverImage())) image = entry.getCoverImage();
                }
            }
        }

        String url = request.getRequestURL().toString();
        String shareImage = toShareImage(image, customDomain);
        String desc = truncate(description, MAX_DESC_LENGTH);
        String locale = en ? "en_US" : "zh_CN";
        String meta = """
                <meta property="og:title" content="%s">
                <meta property="og:description" content="%s">
                <meta property="og:image" content="%s">
                <meta property="og:url" content="%s">
                <meta property="og:type" content="%s">
                <meta property="og:site_name" content="%s">
                <meta property="og:locale" content="%s">
                <meta name="twitter:card" content="summary">
                <meta name="twitter:title" content="%s">
                <meta name="twitter:description" content="%s">
                <meta name="twitter:image" content="%s">
                <meta name="description" content="%s">
                """.formatted(
                escape(title), escape(desc), escape(shareImage),
                escape(url), type, escape(siteName), locale,
                escape(title), escape(desc), escape(shareImage),
                escape(desc));

        html = html.replaceFirst("(?i)<title[^>]*>.*?</title>", "<title>" + escape(title) + "</title>");
        html = html.replace("</head>", meta + "\n</head>");
        return html;
    }

    private Long parseId(String path, String prefix) {
        if (path == null || path.length() <= prefix.length()) return null;
        String rest = path.substring(prefix.length());
        if (rest.contains("/")) return null;
        try {
            return Long.parseLong(rest);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isPublished(String status) {
        return status != null && status.equals("PUBLISHED");
    }

    /**
     * 去掉 Markdown 语法，生成纯文本描述。
     */
    private String stripMarkdown(String text) {
        if (text == null) return "";
        return text
                .replaceAll("```[\\s\\S]*?```", "")
                .replaceAll("`([^`]*)`", "$1")
                .replaceAll("!\\[[^\\]]*\\]\\([^)]*\\)", "")
                .replaceAll("\\[([^\\]]*)\\]\\([^)]*\\)", "$1")
                .replaceAll("(?m)^#{1,6}\\s+", "")
                .replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                .replaceAll("__(.+?)__", "$1")
                .replaceAll("\\*(.+?)\\*", "$1")
                .replaceAll("_(.+?)_", "$1")
                .replaceAll("~~(.+?)~~", "$1")
                .replaceAll("(?m)^>\\s+", "")
                .replaceAll("(?m)^[-*_]{3,}\\s*$", "")
                .replaceAll("(?m)^[\\s]*[-*+]\\s+", "")
                .replaceAll("(?m)^[\\s]*\\d+\\.\\s+", "")
                .replaceAll("<[^>]+>", "")
                .replaceAll("\\n{2,}", " ")
                .replaceAll("\\n", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    /**
     * 对 OSS 图片追加压缩参数（微信卡片对图片大小有要求，压缩后加载更快更稳）。
     * 非 OSS URL 原样返回。
     */
    private String toShareImage(String url, String customDomain) {
        if (!hasText(url) || url.contains("x-oss-process")) return url;
        boolean isOss = url.contains("aliyuncs.com")
                || (hasText(customDomain) && url.startsWith(customDomain));
        if (!isOss) return url;
        String sep = url.contains("?") ? "&" : "?";
        return url + sep + "x-oss-process=image/resize,w_600,m_fill,quality,q_80";
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max) + "…";
    }

    private Map<String, String> loadSettings() {
        try {
            return siteSettingService.getAllSettings();
        } catch (Exception e) {
            log.warn("Failed to load site settings for OG meta: {}", e.getMessage());
            return Map.of();
        }
    }

    private Article findPublished(String slug) {
        try {
            return articleRepository.findBySlug(slug)
                    .filter(a -> a.getStatus() != null && a.getStatus() == 1)
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Failed to load article for OG meta, slug={}: {}", slug, e.getMessage());
            return null;
        }
    }

    private boolean isEnglishRequest(HttpServletRequest request) {
        String lang = request.getHeader("Accept-Language");
        return lang != null && lang.toLowerCase().contains("en");
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private String escape(String s) {
        if (s == null) return "";
        return HtmlUtils.htmlEscape(s);
    }
}
