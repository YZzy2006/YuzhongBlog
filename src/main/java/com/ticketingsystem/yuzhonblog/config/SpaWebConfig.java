package com.ticketingsystem.yuzhonblog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * SPA routing is handled by SpaNavigationFilter.
 * Spring Boot auto-configures static resource serving from classpath:/static/.
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {
}
