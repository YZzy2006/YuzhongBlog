package com.ticketingsystem.yuzhonblog.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketingsystem.yuzhonblog.common.ApiResponse;
import com.ticketingsystem.yuzhonblog.common.ErrorCode;
import com.ticketingsystem.yuzhonblog.security.AiRateLimiter;
import com.ticketingsystem.yuzhonblog.security.ApiRateLimitFilter;
import com.ticketingsystem.yuzhonblog.security.JwtAuthenticationFilter;
import com.ticketingsystem.yuzhonblog.security.LoginRateLimiter;
import com.ticketingsystem.yuzhonblog.security.SecurityHeadersFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final LoginRateLimiter loginRateLimiter;
    private final AiRateLimiter aiRateLimiter;
    private final SecurityHeadersFilter securityHeadersFilter;
    private final ApiRateLimitFilter apiRateLimitFilter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    /**
     * Filter chain 1: 登录端点 - 限流 + 放行
     */
    @Bean
    @Order(1)
    public SecurityFilterChain loginFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/admin/auth/**", "/admin/phone/request-code", "/admin/phone/login")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .addFilterBefore(loginRateLimiter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Filter chain 2: 仅SUPER_ADMIN - AI配置 / 站点设置 / 权限管理
     */
    @Bean
    @Order(2)
    public SecurityFilterChain superAdminFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/admin/ai/**", "/admin/settings/**", "/admin/permissions/**", "/admin/weather/**", "/admin/backups/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("SUPER_ADMIN"))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    String auth = request.getHeader("Authorization");
                    String accept = request.getHeader("Accept");
                    boolean isApiRequest = auth != null ||
                            (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE));
                    if (isApiRequest) {
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.setStatus(401);
                        response.getWriter().write(objectMapper.writeValueAsString(
                                ApiResponse.error(ErrorCode.UNAUTHORIZED)));
                    } else {
                        request.getRequestDispatcher("/index.html").forward(request, response);
                    }
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    String auth = request.getHeader("Authorization");
                    String accept = request.getHeader("Accept");
                    boolean isApiRequest = auth != null ||
                            (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE));
                    if (isApiRequest) {
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.setStatus(403);
                        response.getWriter().write(objectMapper.writeValueAsString(
                                ApiResponse.error(ErrorCode.FORBIDDEN)));
                    } else {
                        request.getRequestDispatcher("/index.html").forward(request, response);
                    }
                })
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(securityHeadersFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Filter chain 3: 管理后台API - 需要JWT认证
     * 使用 /admin/** 通配符，确保所有管理路径都被保护
     * /admin/auth/** 和 /admin/ai/** 由更高优先级的 chain 1/2 先匹配
     *
     * 页面导航（浏览器直接访问）转发到 index.html 由 Vue Router 处理
     * API 请求（带 Authorization 或 Accept: json）返回 401 JSON
     */
    @Bean
    @Order(3)
    public SecurityFilterChain adminApiFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/admin/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    String auth = request.getHeader("Authorization");
                    String accept = request.getHeader("Accept");
                    boolean isApiRequest = auth != null ||
                            (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE));
                    if (isApiRequest) {
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.setStatus(401);
                        response.getWriter().write(objectMapper.writeValueAsString(
                                ApiResponse.error(ErrorCode.UNAUTHORIZED)));
                    } else {
                        // 页面导航 → 转发到 index.html，由 Vue Router 处理重定向
                        request.getRequestDispatcher("/index.html").forward(request, response);
                    }
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setStatus(403);
                    response.getWriter().write(objectMapper.writeValueAsString(
                            ApiResponse.error(ErrorCode.FORBIDDEN)));
                })
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(securityHeadersFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Filter chain 4: 默认 - 公开（前端SPA路由、API、静态资源）+ AI限流
     */
    @Bean
    @Order(4)
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .addFilterBefore(apiRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(aiRateLimiter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(securityHeadersFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                String[] rawOrigins = allowedOrigins.split(",");
                String[] origins = new String[rawOrigins.length];
                for (int i = 0; i < rawOrigins.length; i++) {
                    origins[i] = rawOrigins[i].trim();
                }
                registry.addMapping("/**")
                        .allowedOriginPatterns(origins)
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(false);
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
