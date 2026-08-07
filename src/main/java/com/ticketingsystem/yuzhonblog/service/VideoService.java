package com.ticketingsystem.yuzhonblog.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketingsystem.yuzhonblog.dto.video.VideoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    private final SiteSettingService siteSettingService;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long CACHE_TTL_MS = 3600_000;

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private static final HttpHeaders HEADERS = new HttpHeaders();
    static {
        HEADERS.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36");
        HEADERS.set("Referer", "https://www.bilibili.com/");
    }

    public List<VideoResponse> getVideosByBvids(List<String> bvids) {
        if (bvids == null || bvids.isEmpty()) return Collections.emptyList();

        List<VideoResponse> results = new ArrayList<>();
        for (String bvid : bvids) {
            try {
                VideoResponse video = getVideo(bvid);
                if (video != null) results.add(video);
            } catch (Exception e) {
                log.warn("获取视频 {} 失败: {}", bvid, e.getMessage());
            }
        }
        return results;
    }

    private VideoResponse getVideo(String bvid) throws Exception {
        if (bvid == null || !bvid.matches("^BV[a-zA-Z0-9]{10}$")) {
            log.warn("无效的BV号格式: {}", bvid);
            return null;
        }
        CacheEntry cached = cache.get(bvid);
        if (cached != null && System.currentTimeMillis() - cached.timestamp < CACHE_TTL_MS) {
            return cached.video;
        }

        RestTemplate rt = new RestTemplate();
        rt.setRequestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
            setConnectTimeout(5000);
            setReadTimeout(10000);
        }});

        String url = "https://api.bilibili.com/x/web-interface/view?bvid=" + bvid;
        ResponseEntity<String> resp = rt.exchange(url, HttpMethod.GET, new HttpEntity<>(HEADERS), String.class);
        JsonNode json = MAPPER.readTree(resp.getBody());

        int code = json.path("code").asInt(-1);
        if (code != 0) {
            String msg = json.path("message").asText("unknown error");
            log.warn("Bilibili API 返回错误 (bvid={}): code={}, msg={}", bvid, code, msg);
            return null;
        }

        JsonNode data = json.path("data");
        String title = data.path("title").asText("未知视频");
        String cover = data.path("pic").asText("");
        if (cover.startsWith("//")) cover = "https:" + cover;
        else if (cover.startsWith("http://")) cover = cover.replaceFirst("http://", "https://");
        String author = data.path("owner").path("name").asText("未知UP主");
        int duration = data.path("duration").asInt(0);

        VideoResponse video = VideoResponse.builder()
                .bvid(bvid)
                .title(title)
                .cover(cover)
                .author(author)
                .duration(duration)
                .build();

        cache.put(bvid, new CacheEntry(video, System.currentTimeMillis()));
        return video;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Map<String, String>> getMetaOverrides() {
        String json = siteSettingService.getAllSettings().getOrDefault("video_meta_overrides", "");
        if (json == null || json.isBlank()) return Map.of();
        try {
            return MAPPER.readValue(json, Map.class);
        } catch (Exception e) {
            log.warn("解析video_meta_overrides失败: {}", e.getMessage());
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    public void setMetaOverride(String bvid, String name, String author) {
        String json = siteSettingService.getAllSettings().getOrDefault("video_meta_overrides", "");
        Map<String, Map<String, String>> overrides;
        try {
            overrides = (json == null || json.isBlank()) ? new HashMap<>() : MAPPER.readValue(json, Map.class);
        } catch (Exception e) {
            overrides = new HashMap<>();
        }
        Map<String, String> meta = overrides.getOrDefault(bvid, new HashMap<>());
        if (name != null && !name.isBlank()) meta.put("name", name.trim());
        if (author != null && !author.isBlank()) meta.put("author", author.trim());
        overrides.put(bvid, meta);
        try {
            siteSettingService.setSetting("video_meta_overrides", MAPPER.writeValueAsString(overrides));
        } catch (Exception e) {
            log.error("保存video_meta_overrides失败", e);
        }
        cache.remove(bvid);
    }

    private static class CacheEntry {
        final VideoResponse video;
        final long timestamp;
        CacheEntry(VideoResponse video, long timestamp) {
            this.video = video;
            this.timestamp = timestamp;
        }
    }
}
