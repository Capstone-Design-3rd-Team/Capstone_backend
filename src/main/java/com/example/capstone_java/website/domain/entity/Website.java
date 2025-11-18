package com.example.capstone_java.website.domain.entity;

import com.example.capstone_java.website.domain.event.UrlCrawlEvent;
import com.example.capstone_java.website.global.config.CrawlConfiguration;
import com.example.capstone_java.website.domain.vo.WebsiteId;
import lombok.Getter;

import java.net.MalformedURLException;
import java.net.URL;
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

    // 랜덤 id를 제외하고, 메인url, url 상태, 해당 url을 설정, url 생성시간을 생성
    public static Website create(final String mainUrl) {
        return new Website(
                null,
                mainUrl,
                ExtractionStatus.PENDING,
                CrawlConfiguration.defaultConfiguration(),
                LocalDateTime.now());
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

        // 1. 같은 도메인인지 체크 (가장 먼저!)
        if (!isSameDomain(url)) {
            return false;
        }

        // 2. 제외 경로 확인
        if (this.crawlConfig.shouldExcludePath(url)) {
            return false;
        }

        // 3. 허용 경로 확인
        if (!this.crawlConfig.isAllowedPath(url)) {
            return false;
        }

        return true;
    }

    /**
     * 크롤링 제한에 도달했는지 도메인 로직으로 판단
     */
    public boolean hasReachedCrawlLimits(long currentUrlCount) {
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
            // HTTP/HTTPS 체크
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return false;
            }

            // 다운로드 파일 URL 제외 (파일 다운로드는 크롤링 대상 아님)
            String lowerUrl = url.toLowerCase();
            if (lowerUrl.contains("download") ||
                lowerUrl.contains("file_download") ||
                lowerUrl.endsWith(".pdf") ||
                lowerUrl.endsWith(".zip") ||
                lowerUrl.endsWith(".doc") ||
                lowerUrl.endsWith(".docx") ||
                lowerUrl.endsWith(".xls") ||
                lowerUrl.endsWith(".xlsx") ||
                lowerUrl.endsWith(".ppt") ||
                lowerUrl.endsWith(".pptx") ||
                lowerUrl.endsWith(".hwp") ||
                lowerUrl.endsWith(".jpg") ||
                lowerUrl.endsWith(".png") ||
                lowerUrl.endsWith(".gif") ||
                lowerUrl.endsWith(".mp4") ||
                lowerUrl.endsWith(".avi")) {
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 같은 도메인인지 확인하는 도메인 로직
     * mainUrl의 base domain과 비교 대상 URL의 base domain이 같은지 체크
     */
    private boolean isSameDomain(String url) {
        try {
            URL mainUrlObj = new URL(this.mainUrl);
            URL targetUrlObj = new URL(url);

            String mainHost = mainUrlObj.getHost();
            String targetHost = targetUrlObj.getHost();

            // 정확히 같은 호스트인 경우
            if (mainHost.equals(targetHost)) {
                return true;
            }

            // www 유무 차이만 있는 경우 허용
            // example.com과 www.example.com은 같은 도메인으로 처리
            String mainHostWithoutWww = mainHost.startsWith("www.") ? mainHost.substring(4) : mainHost;
            String targetHostWithoutWww = targetHost.startsWith("www.") ? targetHost.substring(4) : targetHost;

            return mainHostWithoutWww.equals(targetHostWithoutWww);

        } catch (MalformedURLException e) {
            // URL 파싱 실패 시 다른 도메인으로 간주
            return false;
        }
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
