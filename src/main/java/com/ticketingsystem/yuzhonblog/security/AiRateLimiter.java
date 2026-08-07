package com.ticketingsystem.yuzhonblog.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.common.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AiRateLimiter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_MS = 60_000; // 1 minute
    private static final long CLEANUP_INTERVAL = 300_000; // 5 minutes

    private final ConcurrentHashMap<String, AttemptInfo> attempts = new ConcurrentHashMap<>();
    private volatile long lastCleanup = System.currentTimeMillis();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final IpExtractor ipExtractor;

    public AiRateLimiter(IpExtractor ipExtractor) {
        this.ipExtractor = ipExtractor;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/ai/")) {
            filterChain.doFilter(request, response);
            return;
        }

        cleanupExpiredEntries();

        String clientIp = ipExtractor.extractClientIp(request);
        AttemptInfo info = attempts.compute(clientIp, (key, existing) -> {
            if (existing == null || System.currentTimeMillis() - existing.windowStart > WINDOW_MS) {
                return new AttemptInfo();
            }
            return existing;
        });

        if (info.count.incrementAndGet() > MAX_REQUESTS) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(
                    ApiResponse.error(ErrorCode.AI_RATE_LIMITED)));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        if (now - lastCleanup < CLEANUP_INTERVAL) return;
        lastCleanup = now;
        attempts.entrySet().removeIf(entry -> now - entry.getValue().windowStart > WINDOW_MS);
    }

    private static class AttemptInfo {
        final AtomicInteger count = new AtomicInteger(0);
        final long windowStart = System.currentTimeMillis();
    }
}
