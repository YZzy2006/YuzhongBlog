package com.ticketingsystem.yuzhonblog.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter extends OncePerRequestFilter {

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int MAX_REFRESH_ATTEMPTS = 5;
    private static final long WINDOW_MS = 60_000; // 1 minute
    private static final long CLEANUP_INTERVAL = 300_000; // 5 minutes
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ConcurrentHashMap<String, AttemptInfo> attempts = new ConcurrentHashMap<>();
    private volatile long lastCleanup = System.currentTimeMillis();
    private final IpExtractor ipExtractor;

    public LoginRateLimiter(IpExtractor ipExtractor) {
        this.ipExtractor = ipExtractor;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        boolean isLoginEndpoint = uri.equals("/admin/auth/login") && "POST".equals(method);
        boolean isRefreshEndpoint = uri.equals("/admin/auth/refresh") && "POST".equals(method);
        boolean isPhoneCodeEndpoint = uri.equals("/admin/phone/request-code") && "POST".equals(method);
        boolean isPhoneLoginEndpoint = uri.equals("/admin/phone/login") && "POST".equals(method);
        if (!isLoginEndpoint && !isRefreshEndpoint && !isPhoneCodeEndpoint && !isPhoneLoginEndpoint) {
            filterChain.doFilter(request, response);
            return;
        }

        cleanupExpiredEntries();

        String clientIp = ipExtractor.extractClientIp(request);
        int maxAttempts = isRefreshEndpoint ? MAX_REFRESH_ATTEMPTS : MAX_LOGIN_ATTEMPTS;

        // Atomic check-then-increment: use a loop with compute() to ensure
        // the count is only incremented when the request is allowed through.
        // Under contention, CAS retry ensures no request is unfairly rejected.
        boolean allowed = false;
        while (true) {
            AttemptInfo[] holder = new AttemptInfo[1];
            attempts.compute(clientIp, (key, existing) -> {
                if (existing == null || System.currentTimeMillis() - existing.windowStart > WINDOW_MS) {
                    AttemptInfo fresh = new AttemptInfo();
                    fresh.count = 1;
                    holder[0] = fresh;
                    return fresh;
                }
                if (existing.count < maxAttempts) {
                    existing.count++;
                    holder[0] = existing;
                    return existing;
                }
                // Over limit: do not increment, return as-is
                holder[0] = existing;
                return existing;
            });

            AttemptInfo info = holder[0];
            if (info.count <= maxAttempts) {
                allowed = true;
                break;
            }

            // Re-check: the entry might have been replaced by a window reset between
            // our read and the compute. If the window has expired, retry with a fresh window.
            if (System.currentTimeMillis() - info.windowStart > WINDOW_MS) {
                continue; // window expired, retry
            }
            break; // genuinely over limit
        }

        if (!allowed) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            MAPPER.writeValue(response.getWriter(), Map.of(
                    "code", 429,
                    "message", "请求过于频繁，请1分钟后再试",
                    "data", (Object) null
            ));
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
        volatile int count = 1;
        final long windowStart = System.currentTimeMillis();
    }
}
