package com.ticketingsystem.yuzhonblog.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import org.mockito.ArgumentMatchers;

import java.io.IOException;
import java.io.PrintWriter;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginRateLimiterTest {

    private LoginRateLimiter rateLimiter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        rateLimiter = new LoginRateLimiter(new IpExtractor());
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
    }

    @Test
    void doFilter_NonLoginRequest_PassesThrough() throws ServletException, IOException {
        // given
        when(request.getRequestURI()).thenReturn("/api/articles");

        // when
        rateLimiter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void doFilter_LoginRequestUnderLimit_PassesThrough() throws ServletException, IOException {
        // given
        when(request.getRequestURI()).thenReturn("/admin/auth/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        // when - 3 attempts, under the limit of 5
        for (int i = 0; i < 3; i++) {
            rateLimiter.doFilterInternal(request, response, filterChain);
        }

        // then
        verify(filterChain, times(3)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void doFilter_LoginRequestOverLimit_Returns429() throws ServletException, IOException {
        // given
        when(request.getRequestURI()).thenReturn("/admin/auth/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        PrintWriter writer = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(writer);

        // when - exhaust the limit (5 allowed), 6th should fail
        for (int i = 0; i < 5; i++) {
            rateLimiter.doFilterInternal(request, response, filterChain);
        }
        rateLimiter.doFilterInternal(request, response, filterChain);

        // then
        verify(response).setStatus(429);
        verify(response).setContentType("application/json;charset=UTF-8");
        verify(writer).write(ArgumentMatchers.<String>argThat(s -> s.contains("429")));
        verify(filterChain, times(5)).doFilter(request, response);
    }

    @Test
    void doFilter_DifferentIPsHaveSeparateLimits() throws ServletException, IOException {
        // given - two different IPs
        HttpServletRequest requestA = mock(HttpServletRequest.class);
        HttpServletRequest requestB = mock(HttpServletRequest.class);

        when(requestA.getRequestURI()).thenReturn("/admin/auth/login");
        when(requestA.getMethod()).thenReturn("POST");
        when(requestA.getRemoteAddr()).thenReturn("10.0.0.2");

        when(requestB.getRequestURI()).thenReturn("/admin/auth/login");
        when(requestB.getMethod()).thenReturn("POST");
        when(requestB.getRemoteAddr()).thenReturn("10.0.0.3");

        // when - exhaust limit for IP A (5 attempts)
        for (int i = 0; i < 5; i++) {
            rateLimiter.doFilterInternal(requestA, response, filterChain);
        }

        // then - IP B should still be allowed
        rateLimiter.doFilterInternal(requestB, response, filterChain);
        verify(filterChain).doFilter(requestB, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void doFilter_NonPostLoginRequest_PassesThrough() throws ServletException, IOException {
        // given
        when(request.getRequestURI()).thenReturn("/admin/auth/login");
        when(request.getMethod()).thenReturn("GET");

        // when
        rateLimiter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }
}
