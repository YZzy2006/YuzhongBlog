package com.ticketingsystem.yuzhonblog.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class BotDetectionFilter extends OncePerRequestFilter {

    private static final Set<String> BOT_KEYWORDS = Set.of(
            "googlebot", "bingbot", "baiduspider", "yandexbot", "sogou",
            "yahoo", "duckduckbot", "ahrefsbot", "semrushbot", "mj12bot",
            "dotbot", "petalbot", "bytespider", "gptbot", "chatgpt-user",
            "ccbot", "anthropic-ai", "claudebot", "google-extended",
            "facebookbot", "twitterbot", "linkedinbot", "whatsapp",
            "applebot", "ia_archiver", "crawler", "spider", "scraper",
            "bingpreview", "slurp", "exabot", "konqueror", "nutch",
            "baidu", "soso", "yodaobot", "yeti", "meanpathbot",
            "rogerbot", "embedly", "quora", "pinterest", "slackbot",
            "vkShare", "W3C_Validator", "redditbot", "TelegramBot"
    );

    private static final Pattern BOT_UA_PATTERN = Pattern.compile(
            "(?i)(bot|crawl|spider|scrape|slurp|archiver)"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();

        // Only protect HTML pages, not API or static assets
        if (uri.startsWith("/api/") || uri.startsWith("/admin/") ||
            uri.startsWith("/assets/") || uri.startsWith("/favicon") ||
            uri.endsWith(".js") || uri.endsWith(".css") || uri.endsWith(".png") ||
            uri.endsWith(".jpg") || uri.endsWith(".jpeg") || uri.endsWith(".gif") ||
            uri.endsWith(".ico") || uri.endsWith(".svg") || uri.endsWith(".webp") ||
            uri.endsWith(".woff") || uri.endsWith(".woff2") || uri.endsWith(".ttf") ||
            uri.equals("/robots.txt")) {
            filterChain.doFilter(request, response);
            return;
        }

        String userAgent = request.getHeader("User-Agent");

        // Allow empty User-Agent (tunnel proxies may strip it)
        if (userAgent == null || userAgent.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String uaLower = userAgent.toLowerCase();

        // Check against known bot keywords
        for (String keyword : BOT_KEYWORDS) {
            if (uaLower.contains(keyword)) {
                log.warn("Blocked bot request: UA='{}' URI='{}'", userAgent, uri);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("Forbidden");
                return;
            }
        }

        // Regex fallback for unknown bots
        if (BOT_UA_PATTERN.matcher(userAgent).find()) {
            log.warn("Blocked suspicious bot UA: '{}' URI='{}'", userAgent, uri);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Forbidden");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
