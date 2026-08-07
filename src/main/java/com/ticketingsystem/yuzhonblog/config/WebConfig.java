package com.ticketingsystem.yuzhonblog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    // CORS is configured in SecurityConfig.corsConfigurer() to avoid duplicate headers

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Hashed assets (Vite output) — cache aggressively for 1 year
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic());

        // Vendor scripts (jspdf, html2canvas) — cache for 1 year
        registry.addResourceHandler("/vendor/**")
                .addResourceLocations("classpath:/static/vendor/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic());

        // Pet sprites & thumbnails — cache for 30 days (no content-hash in filenames)
        registry.addResourceHandler("/pets/**")
                .addResourceLocations("classpath:/static/pets/")
                .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic());

        // index.html — no cache (always fetch latest for new deploys)
        registry.addResourceHandler("/", "/index.html")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache().cachePublic());
    }
}
