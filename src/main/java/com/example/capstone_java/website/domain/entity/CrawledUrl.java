package com.example.capstone_java.website.domain.entity;

import com.example.capstone_java.website.domain.event.UrlCrawlEvent;
import com.example.capstone_java.website.domain.vo.WebsiteId;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public final class CrawledUrl {
    private final WebsiteId websiteId;
    private final String url;
    private final String parentUrl;
    private final int depth;
    private final CrawlStatus status;
    private final LocalDateTime discoveredAt;
    private final LocalDateTime crawledAt;

    public CrawledUrl(WebsiteId websiteId, String url, String parentUrl, int depth,
                     CrawlStatus status, LocalDateTime discoveredAt, LocalDateTime crawledAt) {
        this.websiteId = websiteId;
        this.url = url;
        this.parentUrl = parentUrl;
        this.depth = depth;
        this.status = status;
        this.discoveredAt = discoveredAt;
        this.crawledAt = crawledAt;
    }

    public static CrawledUrl discovered(WebsiteId websiteId, String url, String parentUrl, int depth) {
        return new CrawledUrl(websiteId, url, parentUrl, depth, CrawlStatus.DISCOVERED, LocalDateTime.now(), null);
    }

    public static CrawledUrl rootUrl(WebsiteId websiteId, String mainUrl) {
        return new CrawledUrl(websiteId, mainUrl, null, 0, CrawlStatus.DISCOVERED, LocalDateTime.now(), null);
    }

    public CrawledUrl markCrawled() {
        return new CrawledUrl(this.websiteId, this.url, this.parentUrl, this.depth,
                             CrawlStatus.CRAWLED, this.discoveredAt, LocalDateTime.now());
    }

    public CrawledUrl markFailed() {
        return new CrawledUrl(this.websiteId, this.url, this.parentUrl, this.depth,
                             CrawlStatus.FAILED, this.discoveredAt, LocalDateTime.now());
    }

    public boolean shouldCrawl(int maxDepth) {
        return this.status == CrawlStatus.DISCOVERED && this.depth < maxDepth;
    }

    public boolean isRoot() {
        return this.parentUrl == null && this.depth == 0;
    }

    // === 도메인 비즈니스 로직 ===

    /**
     * 크롤링이 완료되었는지 도메인 로직으로 판단
     */
    public boolean isCrawlingCompleted() {
        return this.status == CrawlStatus.CRAWLED;
    }

    /**
     * 크롤링이 실패했는지 도메인 로직으로 판단
     */
    public boolean isCrawlingFailed() {
        return this.status == CrawlStatus.FAILED;
    }

    /**
     * 크롤링 대기 중인지 도메인 로직으로 판단
     */
    public boolean isPendingCrawl() {
        return this.status == CrawlStatus.DISCOVERED;
    }

    /**
     * 자식 URL들을 위한 배치 이벤트 생성 가능한지 도메인 로직으로 판단
     */
    public boolean canCreateChildEvents(int maxDepth) {
        return isCrawlingCompleted() && (this.depth + 1) < maxDepth;
    }
}
