package com.ticketingsystem.yuzhonblog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
@EnableCaching
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class YuzhonBlogApplication {

    public static void main(String[] args) {
        SpringApplication.run(YuzhonBlogApplication.class, args);
    }

}
