package com.example.capstone_java.website.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")  // 1. 모든 경로에 대해
                .allowedOriginPatterns("*")     // 2. 모든 Origin 패턴 허용 (http, https, localhost 등)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS") // 3. 허용할 HTTP 메서드
                .allowedHeaders("*")            // 4. 허용할 모든 헤더
                .allowCredentials(true)         // 5. 자격 증명 (쿠키 등) 허용
                .maxAge(3600);                  // 6. Preflight 요청 캐시 시간
    }
}
