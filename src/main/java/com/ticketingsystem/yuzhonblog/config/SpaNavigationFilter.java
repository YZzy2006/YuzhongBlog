package com.ticketingsystem.yuzhonblog.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.ticketingsystem.yuzhonblog.service.OgMetaService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * SPA routing filter:
 * - API requests (JSON, Bearer token) pass through to controllers
 * - Static file requests pass through to resource handlers
 * - All other GET requests forward to index.html for Vue Router
 */
@Slf4j
@Component
@Order(-200)
public class SpaNavigationFilter implements Filter {

    private static final Set<String> STATIC_EXTENSIONS = Set.of(
            ".js", ".css", ".png", ".jpg", ".jpeg", ".gif", ".svg",
            ".ico", ".woff", ".woff2", ".ttf", ".eot", ".map", ".json"
    );

    @Value("${app.spa.index-path:}")
    private String spaIndexPath;

    private final OgMetaService ogMetaService;

    public SpaNavigationFilter(OgMetaService ogMetaService) {
        this.ogMetaService = ogMetaService;
    }

    private volatile byte[] cachedIndexHtml;
    private volatile long cacheTimestamp;
    private static final long CACHE_TTL = 60_000; // 1 minute

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getRequestURI();

        // Only handle GET requests for SPA routing
        if (!"GET".equals(req.getMethod()) || "websocket".equalsIgnoreCase(req.getHeader("Upgrade"))) {
            chain.doFilter(request, response);
            return;
        }

        // Public API paths: always pass through to controllers
        if (path.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        // API calls with JSON accept or auth header: pass through to controllers
        // This catches /admin/** API calls (axios sends Authorization header)
        // but lets browser page navigation (no auth header) fall through to SPA
        String accept = req.getHeader("Accept");
        String auth = req.getHeader("Authorization");
        if (auth != null || (accept != null && accept.contains("application/json"))) {
            chain.doFilter(request, response);
            return;
        }

        // Static file requests: check if file exists, return 204 if not
        if (isStaticFile(path)) {
            ClassPathResource resource = new ClassPathResource("static" + path);
            if (resource.exists()) {
                chain.doFilter(request, response);
            } else {
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }
            return;
        }

        // Root path: serve index.html with OG meta injection (bypass welcome page)
        if (path.equals("/") || path.isEmpty()) {
            serveIndexHtml(req, resp);
            return;
        }

        // Check if the static file actually exists
        ClassPathResource resource = new ClassPathResource("static" + path);
        if (resource.exists()) {
            chain.doFilter(request, response);
            return;
        }

        // All other GET requests: serve index.html for Vue Router
        serveIndexHtml(req, resp);
    }

    private void serveIndexHtml(HttpServletRequest req, HttpServletResponse response) throws IOException {
        byte[] html = getIndexHtml();
        if (html == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "index.html not found");
            return;
        }
        String htmlStr = new String(html, StandardCharsets.UTF_8);
        htmlStr = ogMetaService.inject(htmlStr, req.getRequestURI(), req);
        byte[] out = htmlStr.getBytes(StandardCharsets.UTF_8);
        response.setContentType("text/html; charset=UTF-8");
        response.setContentLength(out.length);
        response.getOutputStream().write(out);
    }

    private byte[] getIndexHtml() {
        // Try filesystem first (production: always up-to-date)
        if (spaIndexPath != null && !spaIndexPath.isBlank()) {
            try {
                long now = System.currentTimeMillis();
                if (cachedIndexHtml != null && now - cacheTimestamp < CACHE_TTL) {
                    return cachedIndexHtml;
                }
                Path filePath = Path.of(spaIndexPath);
                if (Files.exists(filePath)) {
                    byte[] content = Files.readAllBytes(filePath);
                    cachedIndexHtml = content;
                    cacheTimestamp = now;
                    return content;
                }
            } catch (IOException e) {
                log.warn("Failed to read index.html from filesystem: {}", spaIndexPath, e);
            }
        }

        // Fallback: classpath (development)
        try {
            ClassPathResource cpResource = new ClassPathResource("static/index.html");
            if (cpResource.exists()) {
                return cpResource.getInputStream().readAllBytes();
            }
        } catch (IOException e) {
            log.warn("Failed to read index.html from classpath", e);
        }

        return null;
    }

    private boolean isStaticFile(String path) {
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex < 0) return false;
        String ext = path.substring(dotIndex).toLowerCase();
        return STATIC_EXTENSIONS.contains(ext);
    }
}
