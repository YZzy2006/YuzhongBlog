package com.ticketingsystem.yuzhonblog.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;


/**
 * Extracts real client IP from requests behind reverse proxies.
 * Only trusts proxy headers when the direct connection is from a known proxy.
 * Trusted proxies are configurable via `app.trusted-proxies` in application.properties.
 * Defaults to RFC1918 private ranges + loopback. Supports both IPv4 and IPv6.
 */
@Component
public class IpExtractor {

    private static final String[] PROXY_HEADERS = {
            "X-Real-IP",
            "CF-Connecting-IP",
            "X-Forwarded-For"
    };

    @Value("${app.trusted-proxies:127.0.0.1,::1,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16}")
    private List<String> trustedProxies;

    public String extractClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        if (isTrustedProxy(remoteAddr)) {
            for (String header : PROXY_HEADERS) {
                String value = request.getHeader(header);
                if (value != null && !value.isBlank()) {
                    String ip = value.contains(",") ? value.split(",")[0].trim() : value.trim();
                    if (!ip.isEmpty() && !ip.equals("unknown")) {
                        return ip;
                    }
                }
            }
        }

        return remoteAddr;
    }

    private boolean isTrustedProxy(String ip) {
        if (ip == null) return false;
        // Normalize IPv6-mapped IPv4 (e.g. ::ffff:127.0.0.1 -> 127.0.0.1)
        String normalized = normalizeIp(ip);
        for (String proxy : trustedProxies) {
            String trimmed = proxy.trim();
            if (trimmed.isEmpty()) continue;
            String normalizedProxy = normalizeIp(trimmed);
            if (trimmed.contains("/")) {
                if (matchesCidr(normalized, normalizedProxy)) return true;
            } else {
                if (normalized.equalsIgnoreCase(normalizedProxy)) return true;
            }
        }
        return false;
    }

    private String normalizeIp(String ip) {
        if (ip == null) return null;
        // Strip IPv6-mapped IPv4 prefix: ::ffff:1.2.3.4 -> 1.2.3.4
        if (ip.startsWith("::ffff:") && ip.indexOf('.') > 0) {
            return ip.substring(7);
        }
        return ip;
    }

    private boolean matchesCidr(String ip, String cidrWithPrefix) {
        try {
            String[] parts = cidrWithPrefix.split("/");
            String network = parts[0];
            int prefixLen = Integer.parseInt(parts[1]);

            InetAddress ipAddr = InetAddress.getByName(ip);
            InetAddress networkAddr = InetAddress.getByName(network);

            byte[] ipBytes = ipAddr.getAddress();
            byte[] netBytes = networkAddr.getAddress();

            if (ipBytes.length != netBytes.length) return false;

            int fullBytes = prefixLen / 8;
            int remainingBits = prefixLen % 8;

            for (int i = 0; i < fullBytes; i++) {
                if (ipBytes[i] != netBytes[i]) return false;
            }
            if (remainingBits > 0 && fullBytes < ipBytes.length) {
                int mask = (~0) << (8 - remainingBits);
                if ((ipBytes[fullBytes] & mask) != (netBytes[fullBytes] & mask)) return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
