package com.ticketingsystem.yuzhonblog.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketingsystem.yuzhonblog.common.BusinessException;
import com.ticketingsystem.yuzhonblog.common.ErrorCode;
import com.ticketingsystem.yuzhonblog.dto.PageResult;
import com.ticketingsystem.yuzhonblog.dto.backup.BackupResponse;
import com.ticketingsystem.yuzhonblog.dto.backup.ImportSummaryResponse;
import com.ticketingsystem.yuzhonblog.entity.*;
import com.ticketingsystem.yuzhonblog.repository.*;
import com.ticketingsystem.yuzhonblog.security.CurrentUserProvider;
import com.ticketingsystem.yuzhonblog.util.SsrfUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {

    private final ArticleRepository articleRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ArticleTagRepository articleTagRepository;
    private final AnnouncementRepository announcementRepository;
    private final ProjectRepository projectRepository;
    private final TimelineEntryRepository timelineEntryRepository;
    private final FriendLinkRepository friendLinkRepository;
    private final PhotoAlbumRepository photoAlbumRepository;
    private final PhotoRepository photoRepository;
    private final MusicCustomSongRepository musicCustomSongRepository;
    private final SongCoverOverrideRepository songCoverOverrideRepository;
    private final AdminUserRepository adminUserRepository;
    private final SiteSettingRepository siteSettingRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final AiConfigRepository aiConfigRepository;
    private final WeatherConfigRepository weatherConfigRepository;
    private final BackupRecordRepository backupRecordRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${backup.dir:backups}")
    private String backupDir;

    private static final DateTimeFormatter FILE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String INVALID_PASSWORD_HASH = "$2a$10$INVALID_HASH_IMPORTED_USER";

    // ==================== CREATE BACKUP ====================

    @Transactional
    public BackupResponse createBackup(String description) {
        Map<String, Object> data = collectAllData();
        int totalRecords = countRecords(data);

        String filename = "backup_" + LocalDateTime.now().format(FILE_DATE_FMT) + ".json";
        Path dir = Path.of(backupDir);
        Path filePath = dir.resolve(filename);

        try {
            Files.createDirectories(dir);
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
            Files.writeString(filePath, json);
        } catch (IOException e) {
            log.error("Failed to write backup file: {}", filePath, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }

        String username = currentUserProvider.getCurrentUser().getUsername();
        BackupRecord record = new BackupRecord();
        record.setFilename(filename);
        record.setFileSize(fileSize(filePath));
        record.setRecordCount(totalRecords);
        record.setDescription(description);
        record.setCreatedBy(username);
        backupRecordRepository.save(record);

        log.info("Backup created: {} ({} records)", filename, totalRecords);
        return toResponse(record);
    }

    private Map<String, Object> collectAllData() {
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("categories", categoryRepository.findAll().stream().map(this::mapCategory).toList());
        data.put("tags", tagRepository.findAll().stream().map(this::mapTag).toList());
        data.put("articles", articleRepository.findAll().stream().map(this::mapArticle).toList());
        data.put("articleTags", articleTagRepository.findAll().stream().map(this::mapArticleTag).toList());
        data.put("announcements", announcementRepository.findAll().stream().map(this::mapAnnouncement).toList());
        data.put("projects", projectRepository.findAll().stream().map(this::mapProject).toList());
        data.put("timelineEntries", timelineEntryRepository.findAll().stream().map(this::mapTimelineEntry).toList());
        data.put("friendLinks", friendLinkRepository.findAll().stream().map(this::mapFriendLink).toList());
        data.put("photoAlbums", photoAlbumRepository.findAll().stream().map(this::mapPhotoAlbum).toList());
        data.put("photos", photoRepository.findAll().stream().map(this::mapPhoto).toList());
        data.put("musicCustomSongs", musicCustomSongRepository.findAll().stream().map(this::mapMusicCustomSong).toList());
        data.put("songCoverOverrides", songCoverOverrideRepository.findAll().stream().map(this::mapSongCoverOverride).toList());
        data.put("adminUsers", adminUserRepository.findAll().stream().map(this::mapAdminUser).toList());
        data.put("siteSettings", siteSettingRepository.findAll().stream().filter(this::isNotSensitiveSetting).map(this::mapSiteSetting).toList());
        data.put("userPermissions", userPermissionRepository.findAll().stream().map(this::mapUserPermission).toList());
        data.put("aiConfigs", aiConfigRepository.findAll().stream().map(this::mapAiConfig).toList());
        data.put("weatherConfigs", weatherConfigRepository.findAll().stream().map(this::mapWeatherConfig).toList());

        return data;
    }

    private int countRecords(Map<String, Object> data) {
        return data.values().stream()
                .filter(v -> v instanceof List)
                .mapToInt(v -> ((List<?>) v).size())
                .sum();
    }

    // ==================== ENTITY MAPPERS ====================

    private Map<String, Object> mapCategory(Category c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("name", c.getName());
        m.put("sortOrder", c.getSortOrder());
        m.put("createdAt", c.getCreatedAt());
        return m;
    }

    private Map<String, Object> mapTag(Tag t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("name", t.getName());
        m.put("createdAt", t.getCreatedAt());
        return m;
    }

    private Map<String, Object> mapArticle(Article a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("title", a.getTitle());
        m.put("titleEn", a.getTitleEn());
        m.put("contentMd", a.getContentMd());
        m.put("contentMdEn", a.getContentMdEn());
        m.put("contentHtml", a.getContentHtml());
        m.put("contentHtmlEn", a.getContentHtmlEn());
        m.put("summary", a.getSummary());
        m.put("summaryEn", a.getSummaryEn());
        m.put("coverImage", a.getCoverImage());
        m.put("slug", a.getSlug());
        m.put("status", a.getStatus());
        m.put("viewCount", a.getViewCount());
        m.put("likeCount", a.getLikeCount());
        m.put("isTop", a.getIsTop());
        m.put("isFeatured", a.getIsFeatured());
        m.put("cardStyle", a.getCardStyle());
        m.put("authorNotes", a.getAuthorNotes());
        m.put("categoryId", a.getCategory() != null ? a.getCategory().getId() : null);
        m.put("createdAt", a.getCreatedAt());
        return m;
    }

    private Map<String, Object> mapArticleTag(ArticleTag at) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", at.getId());
        m.put("articleId", at.getArticle().getId());
        m.put("tagId", at.getTag().getId());
        return m;
    }

    private Map<String, Object> mapAnnouncement(Announcement a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("tag", a.getTag());
        m.put("tagEn", a.getTagEn());
        m.put("type", a.getType());
        m.put("title", a.getTitle());
        m.put("titleEn", a.getTitleEn());
        m.put("content", a.getContent());
        m.put("contentEn", a.getContentEn());
        m.put("sortOrder", a.getSortOrder());
        m.put("level", a.getLevel());
        m.put("displayStyle", a.getDisplayStyle());
        m.put("active", a.getActive());
        m.put("createdAt", a.getCreatedAt());
        return m;
    }

    private Map<String, Object> mapProject(Project p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("name", p.getName());
        m.put("description", p.getDescription());
        m.put("techStack", p.getTechStack());
        m.put("coverImage", p.getCoverImage());
        m.put("githubUrl", p.getGithubUrl());
        m.put("demoUrl", p.getDemoUrl());
        m.put("sortOrder", p.getSortOrder());
        m.put("subtitle", p.getSubtitle());
        m.put("features", p.getFeatures());
        m.put("subdomainUrl", p.getSubdomainUrl());
        m.put("status", p.getStatus());
        m.put("isFeatured", p.getIsFeatured());
        m.put("createdAt", p.getCreatedAt());
        return m;
    }

    private Map<String, Object> mapTimelineEntry(TimelineEntry t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("title", t.getTitle());
        m.put("description", t.getDescription());
        m.put("coverImage", t.getCoverImage());
        m.put("entryDate", t.getEntryDate());
        m.put("linkUrl", t.getLinkUrl());
        m.put("category", t.getCategory());
        m.put("sortOrder", t.getSortOrder());
        m.put("status", t.getStatus());
        m.put("mood", t.getMood());
        m.put("tags", t.getTags());
        m.put("images", t.getImages());
        m.put("viewCount", t.getViewCount());
        m.put("likeCount", t.getLikeCount());
        m.put("createdAt", t.getCreatedAt());
        return m;
    }

    private Map<String, Object> mapFriendLink(FriendLink f) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", f.getId());
        m.put("name", f.getName());
        m.put("url", f.getUrl());
        m.put("description", f.getDescription());
        m.put("avatar", f.getAvatar());
        m.put("themeColor", f.getThemeColor());
        m.put("sortOrder", f.getSortOrder());
        m.put("createdAt", f.getCreatedAt());
        return m;
    }

    private Map<String, Object> mapPhotoAlbum(PhotoAlbum pa) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", pa.getId());
        m.put("name", pa.getName());
        m.put("description", pa.getDescription());
        m.put("coverUrl", pa.getCoverUrl());
        m.put("sortOrder", pa.getSortOrder());
        m.put("visible", pa.getVisible());
        m.put("createdAt", pa.getCreatedAt());
        return m;
    }

    private Map<String, Object> mapPhoto(Photo p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("albumId", p.getAlbumId());
        m.put("url", p.getUrl());
        m.put("caption", p.getCaption());
        m.put("sortOrder", p.getSortOrder());
        m.put("createdAt", p.getCreatedAt());
        return m;
    }

    private Map<String, Object> mapMusicCustomSong(MusicCustomSong s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("sourceType", s.getSourceType());
        m.put("sourceId", s.getSourceId());
        m.put("title", s.getTitle());
        m.put("artist", s.getArtist());
        m.put("coverUrl", s.getCoverUrl());
        m.put("customCoverUrl", s.getCustomCoverUrl());
        m.put("sortOrder", s.getSortOrder());
        m.put("duration", s.getDuration());
        m.put("createdAt", s.getCreatedAt());
        return m;
    }

    private Map<String, Object> mapSongCoverOverride(SongCoverOverride s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("songId", s.getSongId());
        m.put("customCoverUrl", s.getCustomCoverUrl());
        m.put("createdAt", s.getCreatedAt());
        return m;
    }

    private Map<String, Object> mapAdminUser(AdminUser u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("realName", u.getRealName());
        m.put("email", u.getEmail());
        m.put("role", u.getRole());
        m.put("enabled", u.getEnabled());
        m.put("name", u.getName());
        m.put("bio", u.getBio());
        m.put("avatarUrl", u.getAvatarUrl());
        m.put("createdAt", u.getCreatedAt());
        return m;
    }

    private boolean isNotSensitiveSetting(SiteSetting s) {
        String key = s.getSettingKey().toLowerCase();
        if (key.equals("oss_custom_domain")) return true;
        return !key.contains("password") && !key.contains("secret") && !key.contains("key");
    }

    private Map<String, Object> mapSiteSetting(SiteSetting s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("settingKey", s.getSettingKey());
        m.put("settingValue", s.getSettingValue());
        return m;
    }

    private Map<String, Object> mapUserPermission(UserPermission up) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", up.getId());
        m.put("userId", up.getUserId());
        m.put("permission", up.getPermission());
        m.put("enabled", up.getEnabled());
        return m;
    }

    private Map<String, Object> mapAiConfig(AiConfigEntity a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("name", a.getName());
        m.put("baseUrl", a.getBaseUrl());
        m.put("model", a.getModel());
        m.put("maxTokens", a.getMaxTokens());
        m.put("temperature", a.getTemperature());
        m.put("apiFormat", a.getApiFormat());
        m.put("authType", a.getAuthType());
        m.put("description", a.getDescription());
        m.put("isActive", a.getIsActive());
        m.put("sortOrder", a.getSortOrder());
        m.put("createdAt", a.getCreatedAt());
        return m;
    }

    private Map<String, Object> mapWeatherConfig(WeatherConfigEntity w) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", w.getId());
        m.put("name", w.getName());
        m.put("provider", w.getProvider());
        m.put("baseUrl", w.getBaseUrl());
        m.put("apiFormat", w.getApiFormat());
        m.put("authType", w.getAuthType());
        m.put("language", w.getLanguage());
        m.put("units", w.getUnits());
        m.put("location", w.getLocation());
        m.put("extraParams", w.getExtraParams());
        m.put("description", w.getDescription());
        m.put("isActive", w.getIsActive());
        m.put("createdAt", w.getCreatedAt());
        return m;
    }

    // ==================== LIST / GET / DOWNLOAD ====================

    @Transactional(readOnly = true)
    public PageResult<BackupResponse> listBackups(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<BackupRecord> result = backupRecordRepository.findAllByOrderByIdDesc(PageRequest.of(safePage, safeSize));
        return PageResult.of(result.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public BackupRecord getBackup(Long id) {
        return backupRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BACKUP_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public byte[] downloadBackup(Long id) {
        BackupRecord record = getBackup(id);
        return downloadFile(record.getFilename());
    }

    private byte[] downloadFile(String filename) {
        Path dir = Path.of(backupDir).toAbsolutePath().normalize();
        Path filePath = dir.resolve(filename).normalize();
        if (!filePath.startsWith(dir)) {
            throw new BusinessException(ErrorCode.BACKUP_FILE_NOT_FOUND);
        }
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("Failed to read backup file: {}", filePath, e);
            throw new BusinessException(ErrorCode.BACKUP_FILE_NOT_FOUND);
        }
    }

    // ==================== DELETE ====================

    @Transactional
    public void deleteBackups(List<Long> ids) {
        List<BackupRecord> records = backupRecordRepository.findAllById(ids);
        Path dir = Path.of(backupDir).toAbsolutePath().normalize();
        for (BackupRecord record : records) {
            Path filePath = dir.resolve(record.getFilename()).normalize();
            if (!filePath.startsWith(dir)) {
                log.warn("路径遍历尝试: {}", record.getFilename());
                continue;
            }
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                log.warn("Failed to delete backup file: {}", filePath, e);
            }
        }
        backupRecordRepository.deleteAllById(ids);
    }

    // ==================== IMPORT ====================

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(cacheNames = {"siteInfo", "siteSettings"}, allEntries = true)
    public ImportSummaryResponse importBackup(MultipartFile file) {
        Map<String, List<Map<String, Object>>> data;
        try {
            data = objectMapper.readValue(file.getInputStream(), new TypeReference<>() {});
        } catch (IOException e) {
            log.error("Failed to parse backup file", e);
            throw new BusinessException(ErrorCode.BACKUP_IMPORT_INVALID);
        }

        validateImportData(data);

        Map<String, Integer> counts = new LinkedHashMap<>();

        // 1. Delete in reverse dependency order
        articleTagRepository.deleteAllInBatch();
        articleRepository.deleteAllInBatch();
        tagRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
        announcementRepository.deleteAllInBatch();
        projectRepository.deleteAllInBatch();
        timelineEntryRepository.deleteAllInBatch();
        friendLinkRepository.deleteAllInBatch();
        photoRepository.deleteAllInBatch();
        photoAlbumRepository.deleteAllInBatch();
        musicCustomSongRepository.deleteAllInBatch();
        songCoverOverrideRepository.deleteAllInBatch();
        userPermissionRepository.deleteAllInBatch();
        // Preserve current user: save their data before deletion
        var currentUser = currentUserProvider.getCurrentUser();
        String currentUsername = currentUser.getUsername();
        String currentPasswordHash = currentUser.getPasswordHash();
        String currentRole = currentUser.getRole();
        String currentRealName = currentUser.getRealName();
        String currentEmail = currentUser.getEmail();
        Boolean currentEnabled = currentUser.getEnabled();
        String currentName = currentUser.getName();
        adminUserRepository.deleteAllInBatch();
        siteSettingRepository.deleteAllInBatch();
        aiConfigRepository.deleteAllInBatch();
        weatherConfigRepository.deleteAllInBatch();

        // 2. Import in dependency order with ID remapping

        // Categories first (no dependencies)
        Map<Long, Long> categoryIds = new HashMap<>();
        List<Map<String, Object>> categories = data.getOrDefault("categories", List.of());
        for (Map<String, Object> row : categories) {
            Category c = new Category();
            c.setName((String) row.get("name"));
            c.setSortOrder(toInt(row.get("sortOrder")));
            categoryRepository.save(c);
            categoryIds.put(toLong(row.get("id")), c.getId());
        }
        counts.put("categories", categories.size());

        // Tags (no dependencies)
        Map<Long, Long> tagIds = new HashMap<>();
        List<Map<String, Object>> tags = data.getOrDefault("tags", List.of());
        for (Map<String, Object> row : tags) {
            Tag t = new Tag();
            t.setName((String) row.get("name"));
            tagRepository.save(t);
            tagIds.put(toLong(row.get("id")), t.getId());
        }
        counts.put("tags", tags.size());

        // Articles (depends on Category)
        Map<Long, Long> articleIds = new HashMap<>();
        List<Map<String, Object>> articles = data.getOrDefault("articles", List.of());
        for (Map<String, Object> row : articles) {
            Article a = new Article();
            a.setTitle((String) row.get("title"));
            a.setTitleEn((String) row.get("titleEn"));
            a.setContentMd((String) row.get("contentMd"));
            a.setContentMdEn((String) row.get("contentMdEn"));
            a.setContentHtml((String) row.get("contentHtml"));
            a.setContentHtmlEn((String) row.get("contentHtmlEn"));
            a.setSummary((String) row.get("summary"));
            a.setSummaryEn((String) row.get("summaryEn"));
            a.setCoverImage((String) row.get("coverImage"));
            a.setSlug((String) row.get("slug"));
            a.setStatus(toInt(row.get("status")));
            a.setViewCount(toInt(row.get("viewCount")));
            a.setLikeCount(toInt(row.get("likeCount")));
            a.setIsTop(toInt(row.get("isTop")));
            a.setIsFeatured(toInt(row.get("isFeatured")));
            a.setCardStyle(toInt(row.get("cardStyle")));
            a.setAuthorNotes((String) row.get("authorNotes"));
            Long oldCategoryId = toLong(row.get("categoryId"));
            if (oldCategoryId != null && categoryIds.containsKey(oldCategoryId)) {
                a.setCategory(categoryRepository.getReferenceById(categoryIds.get(oldCategoryId)));
            }
            articleRepository.save(a);
            articleIds.put(toLong(row.get("id")), a.getId());
        }
        counts.put("articles", articles.size());

        // ArticleTags (depends on Article + Tag)
        List<Map<String, Object>> articleTags = data.getOrDefault("articleTags", List.of());
        for (Map<String, Object> row : articleTags) {
            ArticleTag at = new ArticleTag();
            Long oldArticleId = toLong(row.get("articleId"));
            Long oldTagId = toLong(row.get("tagId"));
            if (articleIds.containsKey(oldArticleId) && tagIds.containsKey(oldTagId)) {
                at.setArticle(articleRepository.getReferenceById(articleIds.get(oldArticleId)));
                at.setTag(tagRepository.getReferenceById(tagIds.get(oldTagId)));
                articleTagRepository.save(at);
            }
        }
        counts.put("articleTags", articleTags.size());

        // Announcements
        List<Map<String, Object>> announcements = data.getOrDefault("announcements", List.of());
        for (Map<String, Object> row : announcements) {
            Announcement a = new Announcement();
            a.setTag((String) row.get("tag"));
            a.setTagEn((String) row.get("tagEn"));
            a.setType((String) row.get("type"));
            a.setTitle((String) row.get("title"));
            a.setTitleEn((String) row.get("titleEn"));
            a.setContent((String) row.get("content"));
            a.setContentEn((String) row.get("contentEn"));
            a.setSortOrder(toInt(row.get("sortOrder")));
            a.setLevel((String) row.get("level"));
            a.setDisplayStyle((String) row.get("displayStyle"));
            a.setActive(toBool(row.get("active")));
            announcementRepository.save(a);
        }
        counts.put("announcements", announcements.size());

        // Projects
        List<Map<String, Object>> projects = data.getOrDefault("projects", List.of());
        for (Map<String, Object> row : projects) {
            Project p = new Project();
            p.setName((String) row.get("name"));
            p.setDescription((String) row.get("description"));
            p.setTechStack((String) row.get("techStack"));
            p.setCoverImage((String) row.get("coverImage"));
            p.setGithubUrl((String) row.get("githubUrl"));
            p.setDemoUrl((String) row.get("demoUrl"));
            p.setSortOrder(toInt(row.get("sortOrder")));
            p.setSubtitle((String) row.get("subtitle"));
            p.setFeatures((String) row.get("features"));
            p.setSubdomainUrl((String) row.get("subdomainUrl"));
            p.setStatus((String) row.get("status"));
            p.setIsFeatured(toBool(row.get("isFeatured")));
            projectRepository.save(p);
        }
        counts.put("projects", projects.size());

        // TimelineEntries
        List<Map<String, Object>> timelineEntries = data.getOrDefault("timelineEntries", List.of());
        for (Map<String, Object> row : timelineEntries) {
            TimelineEntry t = new TimelineEntry();
            t.setTitle((String) row.get("title"));
            t.setDescription((String) row.get("description"));
            t.setCoverImage((String) row.get("coverImage"));
            t.setEntryDate(toLocalDate(row.get("entryDate")));
            t.setLinkUrl((String) row.get("linkUrl"));
            t.setCategory((String) row.get("category"));
            t.setSortOrder(toInt(row.get("sortOrder")));
            t.setStatus((String) row.get("status"));
            t.setMood((String) row.get("mood"));
            t.setTags((String) row.get("tags"));
            t.setImages((String) row.get("images"));
            t.setViewCount(toInt(row.get("viewCount")));
            t.setLikeCount(toInt(row.get("likeCount")));
            timelineEntryRepository.save(t);
        }
        counts.put("timelineEntries", timelineEntries.size());

        // FriendLinks
        List<Map<String, Object>> friendLinks = data.getOrDefault("friendLinks", List.of());
        for (Map<String, Object> row : friendLinks) {
            FriendLink f = new FriendLink();
            f.setName((String) row.get("name"));
            f.setUrl((String) row.get("url"));
            f.setDescription((String) row.get("description"));
            f.setAvatar((String) row.get("avatar"));
            f.setThemeColor((String) row.get("themeColor"));
            f.setSortOrder(toInt(row.get("sortOrder")));
            friendLinkRepository.save(f);
        }
        counts.put("friendLinks", friendLinks.size());

        // PhotoAlbums first, then Photos
        Map<Long, Long> albumIds = new HashMap<>();
        List<Map<String, Object>> photoAlbums = data.getOrDefault("photoAlbums", List.of());
        for (Map<String, Object> row : photoAlbums) {
            PhotoAlbum pa = new PhotoAlbum();
            pa.setName((String) row.get("name"));
            pa.setDescription((String) row.get("description"));
            pa.setCoverUrl((String) row.get("coverUrl"));
            pa.setSortOrder(toInt(row.get("sortOrder")));
            pa.setVisible(toBool(row.get("visible")));
            photoAlbumRepository.save(pa);
            albumIds.put(toLong(row.get("id")), pa.getId());
        }
        counts.put("photoAlbums", photoAlbums.size());

        List<Map<String, Object>> photos = data.getOrDefault("photos", List.of());
        for (Map<String, Object> row : photos) {
            Photo p = new Photo();
            Long oldAlbumId = toLong(row.get("albumId"));
            if (oldAlbumId != null && albumIds.containsKey(oldAlbumId)) {
                p.setAlbumId(albumIds.get(oldAlbumId));
            } else {
                p.setAlbumId(oldAlbumId);
            }
            p.setUrl((String) row.get("url"));
            p.setCaption((String) row.get("caption"));
            p.setSortOrder(toInt(row.get("sortOrder")));
            photoRepository.save(p);
        }
        counts.put("photos", photos.size());

        // MusicCustomSongs
        Map<Long, Long> musicSongIds = new HashMap<>();
        List<Map<String, Object>> musicCustomSongs = data.getOrDefault("musicCustomSongs", List.of());
        for (Map<String, Object> row : musicCustomSongs) {
            MusicCustomSong s = new MusicCustomSong();
            s.setSourceType((String) row.get("sourceType"));
            s.setSourceId((String) row.get("sourceId"));
            s.setTitle((String) row.get("title"));
            s.setArtist((String) row.get("artist"));
            s.setCoverUrl((String) row.get("coverUrl"));
            s.setCustomCoverUrl((String) row.get("customCoverUrl"));
            s.setSortOrder(toInt(row.get("sortOrder")));
            s.setDuration(toInt(row.get("duration")));
            musicCustomSongRepository.save(s);
            musicSongIds.put(toLong(row.get("id")), s.getId());
        }
        counts.put("musicCustomSongs", musicCustomSongs.size());

        // SongCoverOverrides
        List<Map<String, Object>> songCoverOverrides = data.getOrDefault("songCoverOverrides", List.of());
        for (Map<String, Object> row : songCoverOverrides) {
            SongCoverOverride s = new SongCoverOverride();
            Long oldSongId = toLong(row.get("songId"));
            if (oldSongId != null && musicSongIds.containsKey(oldSongId)) {
                s.setSongId(musicSongIds.get(oldSongId));
            } else {
                s.setSongId(oldSongId);
            }
            s.setCustomCoverUrl((String) row.get("customCoverUrl"));
            songCoverOverrideRepository.save(s);
        }
        counts.put("songCoverOverrides", songCoverOverrides.size());

        // AdminUsers (with invalid password hash, role capped to 'admin')
        Map<Long, Long> adminUserIds = new HashMap<>();
        boolean currentUserImported = false;
        List<Map<String, Object>> adminUsers = data.getOrDefault("adminUsers", List.of());
        for (Map<String, Object> row : adminUsers) {
            String username = (String) row.get("username");
            if (currentUsername.equals(username)) {
                currentUserImported = true;
            }
            AdminUser u = new AdminUser();
            u.setUsername(username);
            u.setPasswordHash(INVALID_PASSWORD_HASH);
            u.setRealName((String) row.get("realName"));
            u.setEmail((String) row.get("email"));
            String role = (String) row.get("role");
            u.setRole("super_admin".equals(role) ? "admin" : role);
            u.setEnabled(toBool(row.get("enabled")));
            u.setName((String) row.get("name"));
            u.setBio((String) row.get("bio"));
            u.setAvatarUrl((String) row.get("avatarUrl"));
            adminUserRepository.save(u);
            adminUserIds.put(toLong(row.get("id")), u.getId());
        }
        // Re-create current user if not in backup (preserve their real password and role)
        if (!currentUserImported) {
            AdminUser u = new AdminUser();
            u.setUsername(currentUsername);
            u.setPasswordHash(currentPasswordHash);
            u.setRealName(currentRealName);
            u.setEmail(currentEmail);
            u.setRole(currentRole);
            u.setEnabled(currentEnabled);
            u.setName(currentName);
            adminUserRepository.save(u);
        }
        counts.put("adminUsers", adminUsers.size());

        // SiteSettings
        List<Map<String, Object>> siteSettings = data.getOrDefault("siteSettings", List.of());
        for (Map<String, Object> row : siteSettings) {
            SiteSetting s = new SiteSetting();
            s.setSettingKey((String) row.get("settingKey"));
            s.setSettingValue((String) row.get("settingValue"));
            siteSettingRepository.save(s);
        }
        counts.put("siteSettings", siteSettings.size());

        // UserPermissions
        List<Map<String, Object>> userPermissions = data.getOrDefault("userPermissions", List.of());
        for (Map<String, Object> row : userPermissions) {
            UserPermission up = new UserPermission();
            Long oldUserId = toLong(row.get("userId"));
            if (oldUserId != null && adminUserIds.containsKey(oldUserId)) {
                up.setUserId(adminUserIds.get(oldUserId));
            } else {
                up.setUserId(oldUserId);
            }
            up.setPermission((String) row.get("permission"));
            up.setEnabled(toBool(row.get("enabled")));
            userPermissionRepository.save(up);
        }
        counts.put("userPermissions", userPermissions.size());

        // AiConfigs (without apiKey)
        List<Map<String, Object>> aiConfigs = data.getOrDefault("aiConfigs", List.of());
        for (Map<String, Object> row : aiConfigs) {
            AiConfigEntity a = new AiConfigEntity();
            a.setName((String) row.get("name"));
            a.setApiKey("");
            String aiBaseUrl = (String) row.get("baseUrl");
            if (aiBaseUrl != null && !aiBaseUrl.isBlank()) {
                try { SsrfUtil.validateUrl(aiBaseUrl); } catch (Exception e) { aiBaseUrl = ""; }
            }
            a.setBaseUrl(aiBaseUrl);
            a.setModel((String) row.get("model"));
            a.setMaxTokens(toInt(row.get("maxTokens")));
            a.setTemperature(toDouble(row.get("temperature")));
            a.setApiFormat((String) row.get("apiFormat"));
            a.setAuthType((String) row.get("authType"));
            a.setDescription((String) row.get("description"));
            a.setIsActive(toBool(row.get("isActive")));
            a.setSortOrder(toInt(row.get("sortOrder")));
            aiConfigRepository.save(a);
        }
        counts.put("aiConfigs", aiConfigs.size());

        // WeatherConfigs (without apiKey)
        List<Map<String, Object>> weatherConfigs = data.getOrDefault("weatherConfigs", List.of());
        for (Map<String, Object> row : weatherConfigs) {
            WeatherConfigEntity w = new WeatherConfigEntity();
            w.setName((String) row.get("name"));
            w.setProvider((String) row.get("provider"));
            w.setApiKey("");
            String weatherBaseUrl = (String) row.get("baseUrl");
            if (weatherBaseUrl != null && !weatherBaseUrl.isBlank()) {
                try { SsrfUtil.validateUrl(weatherBaseUrl); } catch (Exception e) { weatherBaseUrl = ""; }
            }
            w.setBaseUrl(weatherBaseUrl);
            w.setApiFormat((String) row.get("apiFormat"));
            w.setAuthType((String) row.get("authType"));
            w.setLanguage((String) row.get("language"));
            w.setUnits((String) row.get("units"));
            w.setLocation((String) row.get("location"));
            w.setExtraParams((String) row.get("extraParams"));
            w.setDescription((String) row.get("description"));
            w.setIsActive(toBool(row.get("isActive")));
            weatherConfigRepository.save(w);
        }
        counts.put("weatherConfigs", weatherConfigs.size());

        int totalRecords = counts.values().stream().mapToInt(Integer::intValue).sum();

        ImportSummaryResponse summary = new ImportSummaryResponse();
        summary.setCounts(counts);
        summary.setTotalRecords(totalRecords);

        log.info("Backup imported: {} total records", totalRecords);
        return summary;
    }

    private void validateImportData(Map<String, List<Map<String, Object>>> data) {
        if (data == null || data.isEmpty()) {
            throw new BusinessException(ErrorCode.BACKUP_IMPORT_INVALID);
        }
        if (!data.containsKey("categories") || !data.containsKey("articles")) {
            throw new BusinessException(ErrorCode.BACKUP_IMPORT_INVALID);
        }
    }

    // ==================== HELPERS ====================

    private BackupResponse toResponse(BackupRecord r) {
        BackupResponse resp = new BackupResponse();
        resp.setId(r.getId());
        resp.setFilename(r.getFilename());
        resp.setFileSize(r.getFileSize());
        resp.setRecordCount(r.getRecordCount());
        resp.setDescription(r.getDescription());
        resp.setCreatedBy(r.getCreatedBy());
        resp.setCreatedAt(r.getCreatedAt());
        return resp;
    }

    private Long fileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0L;
        }
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        return Long.parseLong(v.toString());
    }

    private Integer toInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        return Integer.parseInt(v.toString());
    }

    private Boolean toBool(Object v) {
        if (v == null) return null;
        if (v instanceof Boolean b) return b;
        return Boolean.parseBoolean(v.toString());
    }

    private Double toDouble(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.doubleValue();
        return Double.parseDouble(v.toString());
    }

    private LocalDate toLocalDate(Object v) {
        if (v == null) return null;
        if (v instanceof LocalDate ld) return ld;
        return LocalDate.parse(v.toString());
    }
}
