package com.ticketingsystem.yuzhonblog.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.common.ErrorCode;
import com.ticketingsystem.yuzhonblog.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final SessionStore sessionStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            // Parse and validate token ONCE — extract all claims in a single signature verification
            Claims claims;
            try {
                claims = jwtUtil.parseClaims(token);
            } catch (Exception e) {
                // Invalid/expired token — return 401 directly so frontend can handle it
                writeError(response, 401, ErrorCode.TOKEN_INVALID);
                return;
            }

            String tokenType = claims.get("type", String.class);
            if (!"access".equals(tokenType)) {
                writeError(response, 401, ErrorCode.TOKEN_INVALID);
                return;
            }

            Object userIdObj = claims.get("userId");
            Long userId = userIdObj instanceof Number n ? n.longValue() : null;
            if (userId == null) {
                writeError(response, 401, ErrorCode.UNAUTHORIZED);
                return;
            }

            String username = claims.getSubject();
            String role = claims.get("role", String.class);

            // Session conflict check
            SessionStore.SessionData sessionData = sessionStore.getTokenInfo(userId);
            if (sessionData == null) {
                writeError(response, 401, ErrorCode.UNAUTHORIZED);
                return;
            }

            if (!token.equals(sessionData.token())) {
                // Logout endpoint: allow through even on session conflict
                boolean isLogoutRequest = "/admin/logout".equals(request.getRequestURI())
                        && "POST".equalsIgnoreCase(request.getMethod());
                if (isLogoutRequest) {
                    if (username != null && role != null) {
                        List<SimpleGrantedAuthority> auths = List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                        SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(username, null, auths));
                    }
                    filterChain.doFilter(request, response);
                    return;
                }
                // Another device logged in — send kick info
                try {
                    var kickInfo = new LinkedHashMap<String, String>();
                    kickInfo.put("deviceInfo", sessionData.deviceInfo());
                    kickInfo.put("loginTime", sessionData.loginTime());
                    if (role != null) kickInfo.put("role", role);
                    if (sessionData.ip() != null) kickInfo.put("ip", sessionData.ip());
                    if (sessionData.loginMethod() != null) kickInfo.put("loginMethod", sessionData.loginMethod());
                    writeError(response, 409, ErrorCode.SESSION_CONFLICT.getCode(), objectMapper.writeValueAsString(kickInfo));
                } catch (Exception e) {
                    writeError(response, 409, ErrorCode.SESSION_CONFLICT.getCode(), sessionData.deviceInfo());
                }
                return;
            }

            if (role == null) {
                writeError(response, 401, ErrorCode.UNAUTHORIZED);
                return;
            }

            // Build authorities from single parse
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
            Object permsObj = claims.get("permissions");
            if (permsObj instanceof List<?> list) {
                for (Object perm : list) {
                    authorities.add(new SimpleGrantedAuthority("PERM_" + perm));
                }
            }
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(username, null, authorities));
        }

        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, int status, ErrorCode errorCode) throws IOException {
        writeError(response, status, errorCode.getCode(), errorCode.getMessage());
    }

    private void writeError(HttpServletResponse response, int status, int code, String message) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(status);
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.error(code, message)));
    }
}
