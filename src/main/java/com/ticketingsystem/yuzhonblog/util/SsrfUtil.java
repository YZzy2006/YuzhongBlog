package com.ticketingsystem.yuzhonblog.util;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

public final class SsrfUtil {

    private SsrfUtil() {}

    /**
     * Validates that a URL does not target private/reserved IPs.
     * Also resolves DNS to defend against DNS rebinding attacks.
     * Throws IllegalArgumentException if the URL is not allowed.
     */
    public static void validateUrl(String url) {
        if (url == null || url.isBlank()) return;
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null) {
                throw new IllegalArgumentException("Invalid URL: no host");
            }
            // Strip IPv6 brackets for comparison
            if (host.startsWith("[") && host.endsWith("]")) {
                host = host.substring(1, host.length() - 1);
            }
            // Check hostname string against private ranges
            checkHost(host.toLowerCase());

            // DNS rebinding defense: resolve and check the actual IP
            try {
                InetAddress[] addresses = InetAddress.getAllByName(host);
                for (InetAddress addr : addresses) {
                    checkResolvedAddress(addr);
                }
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException("Cannot resolve host: " + host);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid URL: " + e.getMessage());
        }
    }

    private static void checkHost(String host) {
        if (host.equals("localhost") || host.startsWith("127.") || host.startsWith("10.")
                || host.matches("^172\\.(1[6-9]|2[0-9]|3[01])\\..*")
                || host.startsWith("192.168.") || host.equals("169.254.169.254")
                || host.equals("0.0.0.0") || host.equals("::1")
                || host.startsWith("::ffff:") || host.startsWith("0:0:0:0:0:ffff:")
                || host.startsWith("fe80:") || host.startsWith("fc") || host.startsWith("fd")) {
            throw new IllegalArgumentException("URL targets private/reserved IP: " + host);
        }
    }

    private static void checkResolvedAddress(InetAddress addr) {
        if (addr.isLoopbackAddress() || addr.isLinkLocalAddress()
                || addr.isSiteLocalAddress() || addr.isAnyLocalAddress()) {
            throw new IllegalArgumentException("URL resolves to private/reserved IP: " + addr.getHostAddress());
        }
        // Explicitly block cloud metadata endpoint
        byte[] raw = addr.getAddress();
        // 169.254.x.x (link-local for metadata)
        if (raw.length == 4 && (raw[0] & 0xFF) == 169 && (raw[1] & 0xFF) == 254) {
            throw new IllegalArgumentException("URL resolves to metadata endpoint: " + addr.getHostAddress());
        }
    }
}
