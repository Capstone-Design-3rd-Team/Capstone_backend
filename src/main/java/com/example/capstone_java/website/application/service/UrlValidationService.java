package com.example.capstone_java.website.application.service;

import com.example.capstone_java.website.domain.entity.Website;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * URL 검증만 담당하는 단일 책임 서비스
 * - 기본적인 URL 형식 검증
 * - 도메인 정책 적용 (허용/제외 경로)
 * - 중복 URL 필터링
 */
@Slf4j
@Service
public class UrlValidationService {

    /**
     * 기본적인 URL 유효성 검사
     */
    public boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme();

            if (scheme == null) {
                return false;
            }

            // HTTP/HTTPS만 허용
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);

        } catch (IllegalArgumentException e) {
            log.debug("잘못된 URL 형식: {}", url);
            return false;
        }
    }

    /**
     * Website 도메인 정책을 적용한 URL 필터링
     */
    public List<String> filterUrlsByDomainPolicy(List<String> urls, Website website) {
        return urls.stream()
            .filter(this::isValidUrl)                    // 기본 검증
            .filter(website::shouldCrawlUrl)             // 도메인 정책 적용
            .distinct()                                  // 중복 제거
            .collect(Collectors.toList());
    }

    /**
     * 부모 URL과 동일한 URL 제거 (무한 루프 방지)
     */
    public List<String> excludeParentUrl(List<String> urls, String parentUrl) {
        return urls.stream()
            .filter(url -> !url.equals(parentUrl))
            .collect(Collectors.toList());
    }

    /**
     * 동일 도메인 URL만 필터링 (외부 링크 제외)
     */
    public List<String> filterSameDomainUrls(List<String> urls, String baseDomain) {
        return urls.stream()
            .filter(url -> isSameDomain(url, baseDomain))
            .collect(Collectors.toList());
    }

    /**
     * 동일 도메인인지 확인
     */
    private boolean isSameDomain(String url, String baseDomain) {
        try {
            URI urlUri = URI.create(url);
            URI baseUri = URI.create(baseDomain);

            String urlHost = urlUri.getHost();
            String baseHost = baseUri.getHost();

            if (urlHost == null || baseHost == null) {
                return false;
            }

            return urlHost.equalsIgnoreCase(baseHost);

        } catch (Exception e) {
            log.debug("도메인 비교 실패: url={}, base={}", url, baseDomain);
            return false;
        }
    }

    /**
     * URL 목록에서 중복 제거 및 정렬
     */
    public List<String> deduplicateAndSort(List<String> urls) {
        return urls.stream()
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    /**
     * 최대 URL 수 제한 적용
     */
    public List<String> limitUrlCount(List<String> urls, int maxCount) {
        if (urls.size() <= maxCount) {
            return urls;
        }

        log.info("URL 수 제한 적용: {} -> {}", urls.size(), maxCount);
        return urls.stream()
            .limit(maxCount)
            .collect(Collectors.toList());
    }
}