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

    /**
     * URL을 파티션 키로 사용하여 Kafka 파티션 전체에 균등하게 분산
     * → 301개 URL이 6개 파티션에 골고루 분산됨
     */
    @Override
    public String getPartitionKey() {
        return url;  // URL을 파티션 키로 사용
    }

    public static UrlCrawlEvent createRootCrawl(WebsiteId websiteId, String mainUrl) {
        return new UrlCrawlEvent(websiteId, mainUrl, null, 0, LocalDateTime.now());
    }

    public static UrlCrawlEvent createChildCrawl(WebsiteId websiteId, String url, String parentUrl, int depth) {
        return new UrlCrawlEvent(websiteId, url, parentUrl, depth, LocalDateTime.now());
    }
}
