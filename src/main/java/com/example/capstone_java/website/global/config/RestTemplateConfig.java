package com.example.capstone_java.website.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 설정
 * AI 서버와의 HTTP 통신을 위한 빈 구성
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);  // 10초 연결 타임아웃
        factory.setReadTimeout(500000);    // 120초 (2분) 읽기 타임아웃 (AI 분석은 오래 걸림)

        return new RestTemplate(factory);
    }
}