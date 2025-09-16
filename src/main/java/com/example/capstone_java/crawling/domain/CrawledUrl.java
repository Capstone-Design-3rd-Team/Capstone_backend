package com.example.capstone_java.crawling.domain;

import com.example.capstone_java.crawling.domain.vo.CrawlJobId;
import com.example.capstone_java.crawling.domain.vo.CrawledUrlId;
import com.example.capstone_java.crawling.domain.vo.DomainUrl;
import lombok.Getter;

@Getter
public final class CrawledUrl {
    private final CrawledUrlId crawledUrlId;
    private final CrawlJobId jobId; // 어떤 CrawlJob에 속해있는지
    private final DomainUrl url;
    private final CrawledUrlStatus status;

    private CrawledUrl(CrawledUrlId crawledUrlId, CrawlJobId jobId, DomainUrl url, CrawledUrlStatus status) {
        this.crawledUrlId = crawledUrlId;
        this.jobId = jobId;
        this.url = url;
        this.status = status;
    }

    public static CrawledUrl withoutId(CrawlJobId jobId, DomainUrl url) {
        return new CrawledUrl(null, jobId, url, CrawledUrlStatus.PENDING);
    }

    public static CrawledUrl withId(CrawledUrlId crawledUrlId, CrawlJobId jobId, DomainUrl url, CrawledUrlStatus status) {
        return new CrawledUrl(crawledUrlId, jobId, url, status);
    }

    public CrawledUrl processing() {
        if (this.status != CrawledUrlStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태의 URL만 처리할 수 있습니다.");
        }
        return new CrawledUrl(this.crawledUrlId, this.jobId, this.url, CrawledUrlStatus.PROCESSING);
    }

    public CrawledUrl processed() {
        return new CrawledUrl(this.crawledUrlId, this.jobId, this.url, CrawledUrlStatus.PROCESSED);
    }

    public CrawledUrl failed() {
        return new CrawledUrl(this.crawledUrlId, this.jobId, this.url, CrawledUrlStatus.FAILED);
    }
}
