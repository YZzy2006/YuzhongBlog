package com.ticketingsystem.yuzhonblog.service;

import com.ticketingsystem.yuzhonblog.common.BusinessException;
import com.ticketingsystem.yuzhonblog.dto.auth.LoginRequest;
import com.ticketingsystem.yuzhonblog.dto.auth.LoginResponse;
import com.ticketingsystem.yuzhonblog.dto.auth.RefreshRequest;
import com.ticketingsystem.yuzhonblog.entity.AdminUser;
import com.ticketingsystem.yuzhonblog.repository.AdminUserRepository;
import com.ticketingsystem.yuzhonblog.repository.LoginLogRepository;
import com.ticketingsystem.yuzhonblog.security.IpExtractor;
import com.ticketingsystem.yuzhonblog.security.SessionStore;
import com.ticketingsystem.yuzhonblog.util.CaptchaUtil;
import com.ticketingsystem.yuzhonblog.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AdminUserRepository adminUserRepository;
    @Mock
    private LoginLogRepository loginLogRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private SessionStore sessionStore;
    @Mock
    private CaptchaUtil captchaUtil;
    @Mock
    private PermissionService permissionService;
    @Mock
    private IpExtractor ipExtractor;

    @InjectMocks
    private AuthService authService;

    // --- login ---

    @Test
    void login_ValidCredentials_ReturnsTokens() {
        // given
        AdminUser user = buildAdminUser(1L, "admin", "hashed-password");
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("raw-password");
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(ipExtractor.extractClientIp(httpRequest)).thenReturn("127.0.0.1");

        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("raw-password", "hashed-password")).thenReturn(true);
        when(permissionService.getEffectivePermissions(1L, "super_admin")).thenReturn(List.of("all"));
        when(jwtUtil.generateAccessToken("admin", "super_admin", 1L, List.of("all"))).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken("admin")).thenReturn("refresh-token");

        // when
        LoginResponse response = authService.login(request, httpRequest);

        // then
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUsername()).isEqualTo("admin");
        verify(jwtUtil).generateAccessToken("admin", "super_admin", 1L, List.of("all"));
        verify(jwtUtil).generateRefreshToken("admin");
    }

    @Test
    void login_UserNotFound_ThrowsBusinessException() {
        // given
        LoginRequest request = new LoginRequest();
        request.setUsername("unknown");
        request.setPassword("password");
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        when(adminUserRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> authService.login(request, httpRequest))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(1001));
    }

    @Test
    void login_WrongPassword_ThrowsBusinessException() {
        // given
        AdminUser user = buildAdminUser(1L, "admin", "hashed-password");
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("wrong-password");
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        // when / then
        assertThatThrownBy(() -> authService.login(request, httpRequest))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(1001));
    }

    // --- refreshToken ---

    @Test
    void refreshToken_ValidRefreshToken_ReturnsNewTokens() {
        // given
        AdminUser user = buildAdminUser(1L, "admin", "hashed-password");
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("valid-refresh-token");
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getHeader("User-Agent")).thenReturn("test-agent");

        when(jwtUtil.validateTokenType("valid-refresh-token", "refresh")).thenReturn(true);
        when(jwtUtil.getUsernameFromToken("valid-refresh-token")).thenReturn("admin");
        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(sessionStore.validateRefreshToken(1L, "valid-refresh-token")).thenReturn(true);
        when(permissionService.getEffectivePermissions(1L, "super_admin")).thenReturn(List.of("all"));
        when(jwtUtil.generateAccessToken("admin", "super_admin", 1L, List.of("all"))).thenReturn("new-access-token");
        when(jwtUtil.generateRefreshToken("admin")).thenReturn("new-refresh-token");

        // when
        LoginResponse response = authService.refreshToken(request, httpRequest);

        // then
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.getUsername()).isEqualTo("admin");
    }

    @Test
    void refreshToken_InvalidTokenType_ThrowsBusinessException() {
        // given
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("access-token-not-refresh");
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        when(jwtUtil.validateTokenType("access-token-not-refresh", "refresh")).thenReturn(false);

        // when / then
        assertThatThrownBy(() -> authService.refreshToken(request, httpRequest))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(1003));
    }

    @Test
    void refreshToken_UserNotFound_ThrowsBusinessException() {
        // given
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("valid-refresh-token");
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        when(jwtUtil.validateTokenType("valid-refresh-token", "refresh")).thenReturn(true);
        when(jwtUtil.getUsernameFromToken("valid-refresh-token")).thenReturn("deleted-user");
        when(adminUserRepository.findByUsername("deleted-user")).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> authService.refreshToken(request, httpRequest))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(1003));
    }

    @Test
    void refreshToken_ExpiredToken_ThrowsBusinessException() {
        // given
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("expired-token");
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        when(jwtUtil.validateTokenType("expired-token", "refresh")).thenReturn(false);

        // when / then
        assertThatThrownBy(() -> authService.refreshToken(request, httpRequest))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(1003));
    }

    // --- helpers ---

    private AdminUser buildAdminUser(Long id, String username, String passwordHash) {
        AdminUser user = new AdminUser();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setRole("super_admin");
        return user;
    }
}
