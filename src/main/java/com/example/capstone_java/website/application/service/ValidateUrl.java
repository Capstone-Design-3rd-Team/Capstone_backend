package com.example.capstone_java.website.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 포털 사이트 및 크롤링에 부적합한 사이트를 필터링하는 책임을 가진 클래스
 */
@Slf4j
@Component
public class ValidateUrl {
    private static final Set<String> EXCLUDED_DOMAINS = Set.of(
            "naver.com", "daum.net", "kakao.com", "google.com", "youtube.com",
            "facebook.com", "instagram.com", "twitter.com", "x.com", "tistory.com",
            "blog.naver.com", "brunch.co.kr"
    );

    private static final Set<String> EXCLUDED_EXTENSIONS = Set.of(
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".zip", ".hwp", ".jpg", ".jpeg", ".png", ".gif", ".svg"
    );

    public boolean isValidUrl(String url) {

        // 제외 확장자를 가졌는지 확인
        if (EXCLUDED_EXTENSIONS.stream().anyMatch(url::endsWith)) {
            return false;
        }

        // 제외 도메인 목록에 포함되는지 확인 (더 정확한 호스트 비교)
        if (EXCLUDED_DOMAINS.stream().anyMatch(url::endsWith)) {
            return false;
        }

        return true;
    }
}
