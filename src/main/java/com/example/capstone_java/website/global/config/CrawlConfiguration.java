package com.example.capstone_java.website.global.config;

import java.time.Duration;
import java.util.Set;

/**
 * 크롤링 설정을 담는 값 객체
 * 도메인 엔티티가 크롤링 정책을 가지도록 함
 */
public record CrawlConfiguration(
        int maxDepth,           // 최대 크롤링 깊이 (권장: 2-4)
        int maxTotalUrls,       // 전체 사이트당 최대 URL 수 (권장: 1,000-10,000)
        int maxUrlsPerPage,     // 페이지당 최대 추출 URL 수 (권장: 50-100)
        Duration maxDuration,   // 최대 크롤링 시간 (권장: 30분-2시간)
        Set<String> allowedPaths,  // 허용할 경로 패턴 (예: "/products/", "/articles/")
        Set<String> excludedPaths  // 제외할 경로 패턴 (예: "/admin/", "/api/")
) {

    public static CrawlConfiguration defaultConfiguration() {
        // ===== 테스트 설정 (10개 URL만 크롤링) =====
        return new CrawlConfiguration(
                2,                                    // maxDepth (2로 설정: depth 0, 1까지 크롤링 가능)
                10,                                  // maxTotalUrls (10개로 제한)
                10,                                  // maxUrlsPerPage (10개로 제한)
                Duration.ofHours(1),                 // maxDuration
                Set.of(),                            // allowedPaths (모든 경로 허용)
                Set.of("/admin/", "/api/", "/login/") // excludedPaths
        );

        // ===== 운영 설정 (원래 설정 - 주석 처리) =====
        // return new CrawlConfiguration(
        //         5,                                    // maxDepth
        //         5000,                                // maxTotalUrls
        //         100,                                 // maxUrlsPerPage (LMS는 메뉴가 많아 100개로 설정)
        //         Duration.ofHours(1),                 // maxDuration
        //         Set.of(),                            // allowedPaths (모든 경로 허용)
        //         Set.of("/admin/", "/api/", "/login/") // excludedPaths
        // );
    }

    public static CrawlConfiguration create(int maxDepth, int maxTotalUrls) {
        return new CrawlConfiguration(
                maxDepth,
                maxTotalUrls,
                50,
                Duration.ofHours(1),
                Set.of(),
                Set.of("/admin/", "/api/", "/login/")
        );
    }

    public boolean hasReachedMaxDepth(int currentDepth) {
        return currentDepth >= maxDepth;
    }

    public boolean shouldExcludePath(String url) {
        return excludedPaths.stream().anyMatch(url::contains);
    }

    public boolean isAllowedPath(String url) {
        if (allowedPaths.isEmpty()) {
            return true; // 모든 경로 허용
        }
        return allowedPaths.stream().anyMatch(url::contains);
    }
}