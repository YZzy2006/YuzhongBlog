package com.ticketingsystem.yuzhonblog.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * JWT utility. For filter use, prefer {@link #parseClaims(String)} to parse once
 * and extract multiple fields, instead of calling individual getters repeatedly.
 */

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @PostConstruct
    public void validateSecret() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("jwt.secret must be at least 32 characters. Current length: "
                    + (secret == null ? "null" : secret.length()));
        }
    }

    @Value("${jwt.access-token-expiration:7200000}")
    private long accessTokenExpiration; // 2 hours

    @Value("${jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpiration; // 7 days

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String username, String role, Long userId, List<String> permissions) {
        return Jwts.builder()
                .subject(username)
                .claim("type", "access")
                .claim("role", role)
                .claim("userId", userId)
                .claim("permissions", permissions)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(String username) {
        return Jwts.builder()
                .subject(username)
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Parse and validate token once, return all claims.
     * Use this in filters to avoid repeated signature verification.
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public String getRoleFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }

    public Long getUserIdFromToken(String token) {
        Object userId = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("userId");
        if (userId instanceof Number n) return n.longValue();
        return null;
    }

    @SuppressWarnings("unchecked")
    public List<String> getPermissionsFromToken(String token) {
        try {
            Object perms = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("permissions");
            if (perms instanceof List<?> list) {
                return list.stream().map(Object::toString).toList();
            }
        } catch (Exception e) {
            log.warn("Failed to parse permissions from token", e);
        }
        return List.of();
    }

    public String getTokenType(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("type", String.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean validateTokenType(String token, String expectedType) {
        if (!validateToken(token)) return false;
        String type = getTokenType(token);
        return expectedType.equals(type);
    }
}
