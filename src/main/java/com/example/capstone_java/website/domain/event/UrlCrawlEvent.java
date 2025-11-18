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
}
