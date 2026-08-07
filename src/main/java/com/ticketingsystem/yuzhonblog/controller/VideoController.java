package com.ticketingsystem.yuzhonblog.controller;

import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.dto.video.VideoResponse;
import com.ticketingsystem.yuzhonblog.service.SiteSettingService;
import com.ticketingsystem.yuzhonblog.service.VideoService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/video")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;
    private final SiteSettingService siteSettingService;

    private static final HttpHeaders PROXY_HEADERS = new HttpHeaders();
    static {
        PROXY_HEADERS.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        PROXY_HEADERS.set("Referer", "https://www.bilibili.com/");
    }

    @GetMapping("/list")
    public ApiResponse<List<VideoResponse>> list() {
        Map<String, String> allSettings = siteSettingService.getAllSettings();
        String idsStr = allSettings.getOrDefault("video_ids", "").trim();
        if (idsStr.isEmpty()) {
            return ApiResponse.success(Collections.emptyList());
        }

        List<String> bvids = Arrays.stream(idsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        List<VideoResponse> videos = videoService.getVideosByBvids(bvids);
        Map<String, Map<String, String>> overrides = videoService.getMetaOverrides();
        for (VideoResponse video : videos) {
            Map<String, String> meta = overrides.get(video.getBvid());
            if (meta != null) {
                if (meta.containsKey("name") && !meta.get("name").isBlank()) {
                    video.setTitle(meta.get("name"));
                }
                if (meta.containsKey("author") && !meta.get("author").isBlank()) {
                    video.setAuthor(meta.get("author"));
                }
            }
        }
        return ApiResponse.success(videos);
    }

    @GetMapping("/cover")
    public void proxyCover(@RequestParam String url, HttpServletResponse response) {
        if (url == null || url.isBlank() || !isValidBilibiliCoverUrl(url)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
                @Override
                protected void prepareConnection(java.net.HttpURLConnection connection, String httpMethod) throws java.io.IOException {
                    super.prepareConnection(connection, httpMethod);
                    connection.setInstanceFollowRedirects(false);
                }
            };
            factory.setConnectTimeout(5000);
            factory.setReadTimeout(10000);
            RestTemplate rt = new RestTemplate(factory);
            ResponseEntity<byte[]> resp = rt.exchange(url, HttpMethod.GET, new HttpEntity<>(PROXY_HEADERS), byte[].class);
            // Reject redirects (CDN might redirect to an internal URL)
            if (resp.getStatusCode().is3xxRedirection()) {
                log.warn("封面代理拒绝重定向: {} -> {}", url, resp.getHeaders().getLocation());
                response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
                return;
            }
            byte[] data = resp.getBody();
            if (data == null || data.length == 0) {
                response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
                return;
            }
            // Force image content type — don't trust upstream
            response.setContentType(MediaType.IMAGE_JPEG_VALUE);
            response.setHeader("Cache-Control", "public, max-age=86400");
            response.getOutputStream().write(data);
        } catch (Exception e) {
            log.warn("封面代理失败: {} - {}", url, e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
        }
    }

    private static boolean isValidBilibiliCoverUrl(String url) {
        return url.startsWith("https://i0.hdslb.com/")
                || url.startsWith("https://i1.hdslb.com/")
                || url.startsWith("https://i2.hdslb.com/")
                || url.startsWith("https://io.hdslb.com/");
    }
}
