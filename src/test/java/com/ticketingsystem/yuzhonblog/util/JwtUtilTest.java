package com.ticketingsystem.yuzhonblog.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = new JwtUtil();

        Field secretField = JwtUtil.class.getDeclaredField("secret");
        secretField.setAccessible(true);
        secretField.set(jwtUtil, "TestSecretKeyForUnitTests2026!@#$%^&*()_+MustBe256Bits");

        Field accessExpField = JwtUtil.class.getDeclaredField("accessTokenExpiration");
        accessExpField.setAccessible(true);
        accessExpField.set(jwtUtil, 7200000L);

        Field refreshExpField = JwtUtil.class.getDeclaredField("refreshTokenExpiration");
        refreshExpField.setAccessible(true);
        refreshExpField.set(jwtUtil, 604800000L);
    }

    @Test
    void generateAccessToken_ValidUsername_ReturnsTokenWithCorrectSubjectAndType() {
        // when
        String token = jwtUtil.generateAccessToken("admin", "super_admin", 1L, java.util.List.of("all"));

        // then
        assertThat(token).isNotBlank();
        assertThat(jwtUtil.getUsernameFromToken(token)).isEqualTo("admin");
        assertThat(jwtUtil.getTokenType(token)).isEqualTo("access");
    }

    @Test
    void generateRefreshToken_ValidUsername_ReturnsTokenWithCorrectSubjectAndType() {
        // when
        String token = jwtUtil.generateRefreshToken("user123");

        // then
        assertThat(token).isNotBlank();
        assertThat(jwtUtil.getUsernameFromToken(token)).isEqualTo("user123");
        assertThat(jwtUtil.getTokenType(token)).isEqualTo("refresh");
    }

    @Test
    void getUsernameFromToken_ValidToken_ReturnsCorrectUsername() {
        // given
        String token = jwtUtil.generateAccessToken("testuser", "admin", 2L, java.util.List.of("article:view"));

        // when
        String username = jwtUtil.getUsernameFromToken(token);

        // then
        assertThat(username).isEqualTo("testuser");
    }

    @Test
    void getTokenType_AccessToken_ReturnsAccess() {
        // given
        String token = jwtUtil.generateAccessToken("admin", "super_admin", 1L, java.util.List.of("all"));

        // when
        String type = jwtUtil.getTokenType(token);

        // then
        assertThat(type).isEqualTo("access");
    }

    @Test
    void getTokenType_RefreshToken_ReturnsRefresh() {
        // given
        String token = jwtUtil.generateRefreshToken("admin");

        // when
        String type = jwtUtil.getTokenType(token);

        // then
        assertThat(type).isEqualTo("refresh");
    }

    @Test
    void validateToken_ValidToken_ReturnsTrue() {
        // given
        String token = jwtUtil.generateAccessToken("admin", "super_admin", 1L, java.util.List.of("all"));

        // when
        boolean valid = jwtUtil.validateToken(token);

        // then
        assertThat(valid).isTrue();
    }

    @Test
    void validateToken_GarbageString_ReturnsFalse() {
        // when
        boolean valid = jwtUtil.validateToken("this.is.not.a.valid.jwt");

        // then
        assertThat(valid).isFalse();
    }

    @Test
    void validateToken_EmptyString_ReturnsFalse() {
        // when
        boolean valid = jwtUtil.validateToken("");

        // then
        assertThat(valid).isFalse();
    }

    @Test
    void validateTokenType_MatchingType_ReturnsTrue() {
        // given
        String token = jwtUtil.generateAccessToken("admin", "super_admin", 1L, java.util.List.of("all"));

        // when
        boolean valid = jwtUtil.validateTokenType(token, "access");

        // then
        assertThat(valid).isTrue();
    }

    @Test
    void validateTokenType_MismatchedType_ReturnsFalse() {
        // given
        String token = jwtUtil.generateAccessToken("admin", "super_admin", 1L, java.util.List.of("all"));

        // when
        boolean valid = jwtUtil.validateTokenType(token, "refresh");

        // then
        assertThat(valid).isFalse();
    }

    @Test
    void validateTokenType_InvalidToken_ReturnsFalse() {
        // when
        boolean valid = jwtUtil.validateTokenType("invalid.token.here", "access");

        // then
        assertThat(valid).isFalse();
    }
}
