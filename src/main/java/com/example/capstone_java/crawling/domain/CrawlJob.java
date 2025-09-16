package com.example.capstone_java.crawling.domain;

import com.example.capstone_java.crawling.domain.vo.CrawlJobId;
import com.example.capstone_java.crawling.domain.vo.DomainUrl;
import lombok.Getter;

@Getter
public final class CrawlJob {
    private final CrawlJobId crawlJobId;
    private final DomainUrl domainUrl;
    private CrawlJobStatus crawlJobStatus;

    private long discoveredUrlCount; // 지금까지 발견한 총 URL 개수
    private long processedUrlCount;  // 지금까지 처리가 완료된 URL 개수

    // 불변 객체는 상태를 변경할 때마다 새로운 인스턴스를 반환함으로써 멀티스레드 환경에서 데이터의 일관성과 안정성을 보장
    // 쓰레드 안전성을 높인다는 말

    // 이걸 만들어줘야 하는 이유는 urltovisit이랑 visitedurl 정의를 안해주고 만들어주기 때문에 생성자 만들어줘야 함
    private CrawlJob(CrawlJobId jobId, DomainUrl domainUrl, CrawlJobStatus status, long discovered, long processed) {
        this.crawlJobId = jobId;
        this.domainUrl = domainUrl;
        this.crawlJobStatus = status;
        this.discoveredUrlCount = discovered;
        this.processedUrlCount = processed;
    }

    public static CrawlJob create(DomainUrl domainUrl)
    {
        CrawlJob crawlJob = new CrawlJob(
                CrawlJobId.newId(),
                domainUrl,
                CrawlJobStatus.PENDING,
                1L,
                0L);
        return crawlJob;
    }

    public static CrawlJob reconstitute(
            CrawlJobId crawlJobId,
            DomainUrl domainUrl,
            CrawlJobStatus status,
            long discoveredUrlCount,
            long processedUrlCount)
    {
        CrawlJob crawlJob = new CrawlJob(crawlJobId, domainUrl, status, discoveredUrlCount, processedUrlCount);
        return crawlJob;
    }

    public static CrawlJob reconstitute(CrawlJob crawlJob)
    {
        CrawlJob job = new CrawlJob(
                crawlJob.getCrawlJobId(),
                crawlJob.getDomainUrl(),
                crawlJob.getCrawlJobStatus(),
                crawlJob.getDiscoveredUrlCount(),
                crawlJob.getProcessedUrlCount()
        );
        return job;
    }

    public CrawlJob start() {
        if (this.crawlJobStatus != CrawlJobStatus.PENDING) {
            throw new IllegalStateException("이미 시작되었거나 완료된 작업입니다.");
        }
        return new CrawlJob(
                this.crawlJobId,
                this.domainUrl,
                CrawlJobStatus.RUNNING,
                this.discoveredUrlCount,
                this.processedUrlCount
        );
    }

    public CrawlJob incrementDiscoveredCount(long count) {
        if (count <= 0) {
            return this; // 변경이 없으면 현재 인스턴스를 그대로 반환
        }
        long newDiscoveredCount = this.discoveredUrlCount + count;
        return new CrawlJob(
                this.crawlJobId,
                this.domainUrl,
                this.crawlJobStatus,
                newDiscoveredCount, // 변경된 카운트 적용
                this.processedUrlCount
        );
    }

    public CrawlJob incrementProcessedCount() {
        long newProcessedCount = this.processedUrlCount + 1;
        CrawlJobStatus newStatus = this.crawlJobStatus;

        // 핵심 로직: 처리 완료된 개수가 발견된 총 개수와 같아지는 순간, 상태를 COMPLETED로 변경
        if (newProcessedCount >= this.discoveredUrlCount) {
            newStatus = CrawlJobStatus.COMPLETED;
        }

        return new CrawlJob(
                this.crawlJobId,
                this.domainUrl,
                newStatus,
                this.discoveredUrlCount,
                newProcessedCount
        );
    }
}
