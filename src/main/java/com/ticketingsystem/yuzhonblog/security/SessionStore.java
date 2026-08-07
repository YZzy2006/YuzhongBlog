package com.ticketingsystem.yuzhonblog.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class SessionStore {

    private static final long TOKEN_TTL_MS = 7 * 24 * 60 * 60 * 1000L; // 7 days (matches refresh token TTL)

    private final ConcurrentHashMap<String, SessionData> store = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, KickEvent> pendingKicks = new ConcurrentHashMap<>();

    public void storeToken(Long userId, String accessToken, String refreshToken, String userAgent, String role) {
        storeToken(userId, accessToken, refreshToken, userAgent, role, null, null, false);
    }

    public void storeToken(Long userId, String accessToken, String refreshToken, String userAgent, String role, String ip, String loginMethod) {
        storeToken(userId, accessToken, refreshToken, userAgent, role, ip, loginMethod, false);
    }

    public void storeToken(Long userId, String accessToken, String refreshToken, String userAgent, String role, String ip, String loginMethod, boolean isNewLogin) {
        String deviceInfo = parseDeviceInfo(userAgent);
        String loginTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String key = String.valueOf(userId);

        // Only create kick event for new logins from a DIFFERENT device.
        // Same device re-login (e.g. page refresh) should not kick itself.
        SessionData old = store.get(key);
        if (old != null && isNewLogin) {
            String oldDeviceInfo = old.deviceInfo();
            if (!deviceInfo.equals(oldDeviceInfo)) {
                String kickIp = ip != null ? ip : old.ip();
                String kickLoginMethod = loginMethod != null ? loginMethod : old.loginMethod();
                pendingKicks.put(key, new KickEvent(deviceInfo, loginTime, role, kickIp, kickLoginMethod));
                log.info("Kick event stored: userId={}, newDevice={}, oldDevice={}, ip={}", userId, deviceInfo, oldDeviceInfo, kickIp);
            } else {
                log.info("Same device re-login — no kick event: userId={}, device={}", userId, deviceInfo);
            }
        }

        store.put(key, new SessionData(accessToken, refreshToken, deviceInfo, loginTime, System.currentTimeMillis(), ip, loginMethod));
        log.info("Session stored: userId={}, device={}, isNewLogin={}", userId, deviceInfo, isNewLogin);
    }

    /**
     * Consume (retrieve and remove) a pending kick event for the given user.
     * Returns null if no pending kick event exists.
     */
    public KickEvent consumeKickEvent(Long userId) {
        return pendingKicks.remove(String.valueOf(userId));
    }

    /**
     * Peek at a pending kick event without consuming it.
     * Returns null if no pending kick event exists.
     */
    public KickEvent peekKickEvent(Long userId) {
        return pendingKicks.get(String.valueOf(userId));
    }

    public SessionData getTokenInfo(Long userId) {
        SessionData data = store.get(String.valueOf(userId));
        if (data == null) return null;
        if (System.currentTimeMillis() - data.loginTimestamp > TOKEN_TTL_MS) {
            store.remove(String.valueOf(userId));
            return null;
        }
        return data;
    }

    /**
     * Validate that the refresh token matches the stored one.
     * Returns false if session missing, expired, or token mismatch (already consumed/invalidated).
     */
    public boolean validateRefreshToken(Long userId, String refreshToken) {
        SessionData data = getTokenInfo(userId);
        if (data == null) return false;
        return refreshToken.equals(data.refreshToken());
    }

    /**
     * Atomically validate AND consume a refresh token (one-time-use).
     * Prevents concurrent refresh race conditions.
     * Returns true if the token was valid and consumed; false if invalid/already consumed.
     */
    public boolean consumeRefreshToken(Long userId, String refreshToken) {
        String key = String.valueOf(userId);
        SessionData[] consumed = new SessionData[1];
        store.compute(key, (k, existing) -> {
            if (existing == null) return null;
            if (System.currentTimeMillis() - existing.loginTimestamp > TOKEN_TTL_MS) return null;
            if (refreshToken.equals(existing.refreshToken())) {
                consumed[0] = existing;
                return null; // consume — next request will fail
            }
            return existing; // mismatch — don't touch
        });
        return consumed[0] != null;
    }

    public void removeToken(Long userId) {
        String key = String.valueOf(userId);
        store.remove(key);
        pendingKicks.remove(key); // Clean up stale kick events to prevent false kick on next login
        log.info("Session removed: userId={}", userId);
    }

    @Scheduled(fixedRate = 300_000) // every 5 minutes
    public void cleanup() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(entry -> now - entry.getValue().loginTimestamp > TOKEN_TTL_MS);
    }

    public static String parseDeviceInfo(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return "未知设备";

        String ua = userAgent.toLowerCase();
        String device = "PC";
        String os = "Unknown";
        String browser = "Unknown";

        // Device type
        if ((ua.contains("iphone") || ua.contains("android")) && !ua.contains("tablet")) {
            device = "手机";
        } else if (ua.contains("ipad") || ua.contains("tablet")) {
            device = "平板";
        }

        // OS
        if (ua.contains("windows")) os = "Windows";
        else if (ua.contains("mac os")) os = "macOS";
        else if (ua.contains("linux")) os = "Linux";
        else if (ua.contains("android")) os = "Android";
        else if (ua.contains("iphone") || ua.contains("ipad")) os = "iOS";

        // Browser
        if (ua.contains("edg/")) browser = "Edge";
        else if (ua.contains("chrome") && !ua.contains("edg")) browser = "Chrome";
        else if (ua.contains("firefox")) browser = "Firefox";
        else if (ua.contains("safari") && !ua.contains("chrome")) browser = "Safari";

        return browser + " · " + os + " · " + device;
    }

    public record SessionData(String token, String refreshToken, String deviceInfo, String loginTime, long loginTimestamp, String ip, String loginMethod) {}

    public record KickEvent(String deviceInfo, String loginTime, String role, String ip, String loginMethod) {}
}
