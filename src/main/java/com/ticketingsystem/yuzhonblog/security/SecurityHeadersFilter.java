package com.ticketingsystem.yuzhonblog.security;

import com.ticketingsystem.yuzhonblog.service.OssConfigService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private final OssConfigService ossConfigService;

    @Value("${aliyun.oss.endpoint:}")
    private String ossEndpoint;

    @Value("${aliyun.oss.bucket-name:}")
    private String ossBucket;

    private volatile String cachedOssOrigin;
    private volatile long cacheExpiry;

    public SecurityHeadersFilter(OssConfigService ossConfigService) {
        this.ossConfigService = ossConfigService;
    }

    private String getOssOrigin() {
        long now = System.currentTimeMillis();
        if (cachedOssOrigin != null && now < cacheExpiry) return cachedOssOrigin;

        // Build OSS origins for CSP — prefer database config, fall back to properties
        String endpoint = ossConfigService.getEndpoint();
        String bucket = ossConfigService.getBucketName();
        String customDomain = ossConfigService.getCustomDomain();

        if (endpoint == null || endpoint.isBlank()) endpoint = ossEndpoint;
        if (bucket == null || bucket.isBlank()) bucket = ossBucket;

        StringBuilder sb = new StringBuilder();
        if (endpoint != null && !endpoint.isBlank() && bucket != null && !bucket.isBlank()) {
            sb.append(" https://").append(bucket).append(".").append(endpoint);
        }
        if (customDomain != null && !customDomain.isBlank()) {
            String domain = customDomain.replace("https://", "").replace("http://", "").replaceFirst("/$", "");
            sb.append(" https://").append(domain);
        }

        cachedOssOrigin = sb.toString();
        cacheExpiry = now + 300_000; // 5 minutes
        return cachedOssOrigin;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ossOrigin = getOssOrigin();

        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-Robots-Tag", "noindex, nofollow");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        response.setHeader("Content-Security-Policy",
                "default-src 'self'; " +
                "script-src 'self' https://unpkg.com; " +
                "style-src 'self' 'unsafe-inline' https://unpkg.com; " +
                "img-src 'self' data: blob: https://p1.music.126.net https://p2.music.126.net https://p3.music.126.net https://i0.hdslb.com https://i1.hdslb.com https://i2.hdslb.com" + ossOrigin + "; " +
                "font-src 'self' data: https://unpkg.com; " +
                "connect-src 'self' https://api.open-meteo.com" + ossOrigin + "; " +
                "media-src 'self' data: https://music.163.com https://*.music.126.net; " +
                "frame-src 'self' https://player.bilibili.com https://www.bilibili.com; " +
                "frame-ancestors 'none'; " +
                "object-src 'none'; " +
                "base-uri 'self'");
        filterChain.doFilter(request, response);
    }
}
