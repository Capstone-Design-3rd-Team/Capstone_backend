package com.example.capstone_java.website.domain.entity;

import com.example.capstone_java.website.domain.event.UrlCrawlEvent;
import com.example.capstone_java.website.domain.vo.CrawlConfiguration;
import com.example.capstone_java.website.domain.vo.WebsiteId;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public final class Website {
    private final WebsiteId websiteId;
    private final String mainUrl;
    private final ExtractionStatus extractionStatus;
    private final CrawlConfiguration crawlConfig;
    private final LocalDateTime createdAt;

    // MapStruct용 public 생성자 (하나만 유지)
    public Website(WebsiteId websiteId, String mainUrl, ExtractionStatus extractionStatus, CrawlConfiguration crawlConfig, LocalDateTime createdAt) {
        this.websiteId = websiteId;
        this.mainUrl = mainUrl;
        this.extractionStatus = extractionStatus;
        this.crawlConfig = crawlConfig;
        this.createdAt = createdAt;
    }

    public static Website create(final String mainUrl) {
        return new Website(null, mainUrl, ExtractionStatus.PENDING, CrawlConfiguration.defaultConfiguration(), LocalDateTime.now());
    }

    public static Website create(final String mainUrl, final CrawlConfiguration crawlConfig) {
        return new Website(null, mainUrl, ExtractionStatus.PENDING, crawlConfig, LocalDateTime.now());
    }

    public static Website withId(final WebsiteId websiteId,
                                 final String mainUrl,
                                 final ExtractionStatus extractionStatus,
                                 final CrawlConfiguration crawlConfig,
                                 final LocalDateTime creationDateTime)
    {
        return new Website(websiteId, mainUrl, extractionStatus, crawlConfig, creationDateTime);
    }

    public Website startExtraction() {
        if (this.extractionStatus != ExtractionStatus.PENDING) {
            throw new IllegalStateException("추출이 이미 시작되었습니다.");
        }
        return new Website(this.websiteId, this.mainUrl, ExtractionStatus.PROGRESS, this.crawlConfig, this.createdAt);
    }

    public Website markCompleted() {
        return new Website(this.websiteId, this.mainUrl, ExtractionStatus.COMPLETE, this.crawlConfig, this.createdAt);
    }

    public Website markFailed() {
        return new Website(this.websiteId, this.mainUrl, ExtractionStatus.FAILED, this.crawlConfig, this.createdAt);
    }

    public boolean isCompleted() {
        return extractionStatus == ExtractionStatus.COMPLETE;
    }

    public boolean isFailed() {
        return extractionStatus == ExtractionStatus.FAILED;
    }

    public boolean isInProgress() {
        return extractionStatus == ExtractionStatus.PROGRESS;
    }

    public boolean isPending() {
        return extractionStatus == ExtractionStatus.PENDING;
    }

    // === 도메인 비즈니스 로직 ===

    /**
     * 크롤링을 시작할 수 있는지 도메인 로직으로 판단
     */
    public boolean canStartCrawling() {
        return this.extractionStatus == ExtractionStatus.PENDING;
    }

    /**
     * 루트 URL 크롤링 이벤트 생성 (도메인 로직)
     */
    public UrlCrawlEvent createRootCrawlEvent() {
        if (!canStartCrawling()) {
            throw new IllegalStateException("크롤링을 시작할 수 없는 상태입니다: " + this.extractionStatus);
        }
        return UrlCrawlEvent.createRootCrawl(this.websiteId, this.mainUrl, this.crawlConfig.maxDepth());
    }

    /**
     * 특정 깊이가 크롤링 가능한지 도메인 로직으로 판단
     */
    public boolean canCrawlAtDepth(int depth) {
        return depth < this.crawlConfig.maxDepth();
    }

    /**
     * URL이 크롤링 허용 대상인지 도메인 로직으로 판단
     */
    public boolean shouldCrawlUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        // 제외 경로 확인
        if (this.crawlConfig.shouldExcludePath(url)) {
            return false;
        }

        // 허용 경로 확인
        if (!this.crawlConfig.isAllowedPath(url)) {
            return false;
        }

        return true;
    }

    /**
     * 크롤링 제한에 도달했는지 도메인 로직으로 판단
     */
    public boolean hasReachedCrawlLimits(int currentUrlCount) {
        return currentUrlCount >= this.crawlConfig.maxTotalUrls();
    }

    /**
     * URL 목록을 도메인 정책에 따라 필터링 (진짜 DDD 방식)
     */
    public List<String> filterValidUrls(List<String> urls) {
        return urls.stream()
            .filter(this::isValidUrl)           // 기본 형식 검증
            .filter(this::shouldCrawlUrl)       // 도메인 정책 적용
            .distinct()                         // 중복 제거
            .limit(this.crawlConfig.maxUrlsPerPage())  // 페이지당 URL 제한
            .collect(Collectors.toList());
    }

    /**
     * 부모 URL과 동일한 URL 제거하는 도메인 로직
     */
    public List<String> excludeParentUrl(List<String> urls, String parentUrl) {
        return urls.stream()
            .filter(url -> !url.equals(parentUrl))
            .collect(Collectors.toList());
    }

    /**
     * 기본적인 URL 형식 검증 (도메인 로직)
     */
    private boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        try {
            return url.startsWith("http://") || url.startsWith("https://");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 발견된 URL들로부터 크롤링 이벤트들을 생성하는 도메인 로직
     */
    public List<UrlCrawlEvent> createChildCrawlEvents(List<String> urls, String parentUrl, int currentDepth) {
        return urls.stream()
            .map(url -> UrlCrawlEvent.createChildCrawl(
                this.websiteId,
                url,
                parentUrl,
                currentDepth + 1,
                this.crawlConfig.maxDepth()
            ))
            .collect(Collectors.toList());
    }

    /**
     * 크롤링 통계 정보를 반환하는 도메인 로직
     */
    public String getCrawlingSummary() {
        return String.format("Website[%s] - MaxDepth: %d, MaxUrls: %d, Status: %s",
            this.mainUrl,
            this.crawlConfig.maxDepth(),
            this.crawlConfig.maxTotalUrls(),
            this.extractionStatus
        );
    }
}
