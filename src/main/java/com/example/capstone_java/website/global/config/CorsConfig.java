package com.example.capstone_java.website.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "https://tech-for-everyone-virid.vercel.app", // 1. 실제 배포된 프론트 도메인 (필수!)
                        "http://localhost:3000"                         // 2. 로컬 개발 환경 (테스트용)

                )
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true) // SSE나 토큰 사용 시 필수
                .maxAge(3600);
    }
}
