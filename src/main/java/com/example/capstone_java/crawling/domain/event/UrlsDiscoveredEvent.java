package com.example.capstone_java.crawling.domain.event;

import com.example.capstone_java.crawling.domain.vo.CrawlJobId;
import com.example.capstone_java.crawling.domain.vo.DomainUrl;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record UrlsDiscoveredEvent(UUID jobId, Set<String> discoveredUrls) {
    // PageFetchingConsumer가 DomainUrl Set을 받았을 때 쉽게 이벤트를 생성하도록 돕는 생성자
    public static UrlsDiscoveredEvent fromDomain(CrawlJobId crawlJobId, Set<DomainUrl> domainUrls) {
        Set<String> urlStrings = domainUrls.stream()
                .map(DomainUrl::url)
                .collect(Collectors.toSet());
        return new UrlsDiscoveredEvent(crawlJobId.getUuid(), urlStrings);
    }
}
