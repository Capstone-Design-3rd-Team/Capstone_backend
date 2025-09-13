package com.example.capstone_java.crawling.domain;

import com.example.capstone_java.crawling.domain.vo.CrawlJobId;
import com.example.capstone_java.crawling.domain.vo.DomainUrl;
import lombok.Getter;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

@Getter
public class CrawlJob {
    private final CrawlJobId crawlJobId;
    private final DomainUrl domainUrl;
    private CrawlJobStatus crawlJobStatus;
    private final Queue<DomainUrl> urlsToVisit = new LinkedList<>();
    private final Set<DomainUrl> visitedUrl = new HashSet<>();

    // 불변 객체는 상태를 변경할 때마다 새로운 인스턴스를 반환함으로써 멀티스레드 환경에서 데이터의 일관성과 안정성을 보장
    // 쓰레드 안전성을 높인다는 말

    // 이걸 만들어줘야 하는 이유는 urltovisit이랑 visitedurl 정의를 안해주고 만들어주기 때문에 생성자 만들어줘야 함
    private CrawlJob(CrawlJobId jobId, DomainUrl domainUrl, CrawlJobStatus status) {
        this.crawlJobId = jobId;
        this.domainUrl = domainUrl;
        this.crawlJobStatus = status;
    }

    public static CrawlJob create(DomainUrl domainUrl) {
        return new CrawlJob(CrawlJobId.newId(), domainUrl, CrawlJobStatus.PENDING);
    }

    public static CrawlJob reconstitute(CrawlJobId crawlJobId, DomainUrl domainUrl, CrawlJobStatus status) {
        return new CrawlJob(crawlJobId, domainUrl, status);
    }

    public void start() {
        if(this.crawlJobStatus != CrawlJobStatus.PENDING) {
            throw new IllegalStateException("이미 시작되었거나 완료된 작업입니다");
        }
        this.crawlJobStatus = CrawlJobStatus.RUNNING;
        this.urlsToVisit.add(this.domainUrl);
        this.visitedUrl.add(this.domainUrl);
    }

    public DomainUrl nextUrlToVisit() {
       return this.urlsToVisit.poll();
    }

    public void addDiscoveredUrls(Set<DomainUrl> discoveredUrls) {
        if (this.crawlJobStatus != CrawlJobStatus.RUNNING) return;

        for (DomainUrl newUrl : discoveredUrls) {
            if (domainUrl.isSameDomain(newUrl) && visitedUrl.add(newUrl)) {
                urlsToVisit.add(newUrl);
            }
        }
    }

    public boolean hasMoreUrlsToVisit() {
        return !urlsToVisit.isEmpty();
    }

    public void complete() {
        if (!hasMoreUrlsToVisit()) {
            this.crawlJobStatus = CrawlJobStatus.COMPLETED;
        }
    }
}
