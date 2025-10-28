package com.example.capstone_java.website.domain.event;

import com.example.capstone_java.website.domain.vo.WebsiteId;

import java.time.LocalDateTime;

public record UrlCrawlEvent(
        WebsiteId websiteId,
        String url,
        String parentUrl,
        int depth,
        LocalDateTime eventOccurredAt
) implements DomainEvent {

    @Override
    public LocalDateTime occurredAt() {
        return eventOccurredAt;
    }

    public static UrlCrawlEvent createRootCrawl(WebsiteId websiteId, String mainUrl) {
        return new UrlCrawlEvent(websiteId, mainUrl, null, 0, LocalDateTime.now());
    }

    public static UrlCrawlEvent createChildCrawl(WebsiteId websiteId, String url, String parentUrl, int depth) {
        return new UrlCrawlEvent(websiteId, url, parentUrl, depth, LocalDateTime.now());
    }

    /**
     * 파티션 키: URL 자체를 사용하여 균등 분산
     * - 같은 URL은 항상 같은 파티션 (멱등성 보장)
     * - 다른 URL은 골고루 분산 (병렬 처리)
     */
    public String getPartitionKey() {
        return this.url;
    }
}
