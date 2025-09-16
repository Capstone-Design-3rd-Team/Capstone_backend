package com.example.capstone_java.crawling.application.service;

import com.example.capstone_java.crawling.application.port.in.command.CrawlCommand;
import com.example.capstone_java.crawling.application.port.in.usecase.CrawlUseCase;
import com.example.capstone_java.crawling.application.port.out.SaveCrawlJobPort;
import com.example.capstone_java.crawling.application.port.out.SaveCrawledUrlPort;
import com.example.capstone_java.crawling.application.port.out.UrlFetchEventPublisherPort;
import com.example.capstone_java.crawling.domain.CrawlJob;
import com.example.capstone_java.crawling.domain.CrawledUrl;
import com.example.capstone_java.crawling.domain.event.UrlFetchEvent;
import com.example.capstone_java.crawling.domain.vo.DomainUrl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrawlCommandService implements CrawlUseCase {
    private final SaveCrawlJobPort saveCrawlJobPort;
    private final SaveCrawledUrlPort saveCrawledUrlPort;
    private final UrlFetchEventPublisherPort urlToFetchEventPublisherPort;

    /**
     * 새로운 크롤링 작업을 시작합니다.
     * 이 메서드는 작업 생성 및 첫 이벤트 발행까지의 과정을 하나의 트랜잭션으로 묶습니다.
     * @param command 크롤링 시작에 필요한 데이터 (시작 URL)
     * @return 생성된 크롤링 작업의 고유 ID
     */
    @Override
    @Transactional
    public UUID startCrawling(CrawlCommand command) {
        DomainUrl domainUrl = new DomainUrl(command.url());
        // crawlJob
        CrawlJob crawlJob = CrawlJob.create(domainUrl);
        CrawlJob savedCrawlJob = saveCrawlJobPort.save(crawlJob);
        // crawledUrl
        CrawledUrl firstUrl = CrawledUrl.withoutId(savedCrawlJob.getCrawlJobId(), domainUrl);
        CrawledUrl savedFirstUrl = saveCrawledUrlPort.save(firstUrl);

        urlToFetchEventPublisherPort.publish(
                new UrlFetchEvent(savedCrawlJob.getCrawlJobId(), domainUrl)
        );
        return savedCrawlJob.getCrawlJobId().getUuid();
    }
}
