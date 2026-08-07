package com.ticketingsystem.yuzhonblog.security;

import com.ticketingsystem.yuzhonblog.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private SessionStore sessionStore;

    private JwtAuthenticationFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtUtil, sessionStore);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_NoAuthHeader_CallsFilterChainWithoutAuthentication() throws ServletException, IOException {
        // given
        when(request.getHeader("Authorization")).thenReturn(null);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_ValidAccessToken_SetsAuthenticationWithCorrectUsername() throws ServletException, IOException {
        // given
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-access-token");
        when(jwtUtil.validateTokenType("valid-access-token", "access")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("valid-access-token")).thenReturn(1L);
        when(jwtUtil.getUsernameFromToken("valid-access-token")).thenReturn("admin");
        when(jwtUtil.getRoleFromToken("valid-access-token")).thenReturn("super_admin");
        when(jwtUtil.getPermissionsFromToken("valid-access-token")).thenReturn(java.util.List.of("all"));
        when(sessionStore.getTokenInfo(1L)).thenReturn(
                new SessionStore.SessionData("valid-access-token", "refresh", "test", "now", 0L, "", ""));

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("admin");
    }

    @Test
    void doFilter_InvalidToken_NoAuthenticationSet() throws ServletException, IOException {
        // given
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(jwtUtil.validateTokenType("invalid-token", "access")).thenReturn(false);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_RefreshTokenUsedAsAccess_NoAuthenticationSet() throws ServletException, IOException {
        // given
        when(request.getHeader("Authorization")).thenReturn("Bearer refresh-token");
        when(jwtUtil.validateTokenType("refresh-token", "access")).thenReturn(false);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_AuthHeaderWithoutBearerPrefix_NoAuthenticationSet() throws ServletException, IOException {
        // given
        when(request.getHeader("Authorization")).thenReturn("Token some-token-value");

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtUtil);
    }
}
