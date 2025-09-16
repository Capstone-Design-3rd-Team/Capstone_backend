package com.example.capstone_java.crawling.domain.event;

import com.example.capstone_java.crawling.domain.vo.CrawlJobId;
import com.example.capstone_java.crawling.domain.vo.DomainUrl;

import java.util.UUID;

public record UrlFetchEvent(UUID jobId, String url) {
    public UrlFetchEvent(CrawlJobId crawlJobId, DomainUrl domainUrl) {
        this(crawlJobId.getUuid(), domainUrl.url());
    }
}
