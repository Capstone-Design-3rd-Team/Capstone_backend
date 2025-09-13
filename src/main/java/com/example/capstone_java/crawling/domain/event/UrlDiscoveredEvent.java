package com.example.capstone_java.crawling.domain.event;

import com.example.capstone_java.crawling.domain.vo.CrawlJobId;
import com.example.capstone_java.crawling.domain.vo.DomainUrl;

import java.util.UUID;

public record UrlDiscoveredEvent(UUID jobId, String url) {
    public UrlDiscoveredEvent(CrawlJobId crawlJobId, DomainUrl domainUrl) {
        this(crawlJobId.getUuid(), domainUrl.url());
    }
}
