package com.example.capstone_java.website.domain.event;

import com.example.capstone_java.website.domain.vo.WebsiteId;

import java.time.LocalDateTime;

public record UrlCrawlEvent(
        WebsiteId websiteId,
        String url,
        String parentUrl,
        int depth,
        int maxDepth,
        LocalDateTime eventOccurredAt
) implements DomainEvent {

    @Override
    public LocalDateTime occurredAt() {
        return eventOccurredAt;
    }

    public static UrlCrawlEvent createRootCrawl(WebsiteId websiteId, String mainUrl, int maxDepth) {
        return new UrlCrawlEvent(websiteId, mainUrl, null, 0, maxDepth, LocalDateTime.now());
    }

    public static UrlCrawlEvent createChildCrawl(WebsiteId websiteId, String url, String parentUrl, int depth, int maxDepth) {
        return new UrlCrawlEvent(websiteId, url, parentUrl, depth, maxDepth, LocalDateTime.now());
    }

    public String getPartitionKey() {
        return websiteId != null ? websiteId.getId().toString() : "default";
    }
}
