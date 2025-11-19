package com.example.capstone_java.website.adapter.in.kafka;

import com.example.capstone_java.website.application.port.out.GetWebsitePort;
import com.example.capstone_java.website.application.port.out.SaveWebsitePort;
import com.example.capstone_java.website.domain.entity.Website;
import com.example.capstone_java.website.domain.event.ExtractionStartedEvent;
import com.example.capstone_java.website.domain.event.UrlCrawlEvent;
import com.example.capstone_java.website.global.common.KafkaGroups;
import com.example.capstone_java.website.global.common.KafkaTopics;
import com.example.capstone_java.website.application.event.EventDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExtractionEventConsumer {

    private final GetWebsitePort getWebsitePort;
    private final SaveWebsitePort saveWebsitePort;
    private final EventDispatcher eventDispatcher;

    @RetryableTopic(
        attempts = "1",
        backoff = @Backoff(delay = 1000, multiplier = 2.0),
        dltTopicSuffix = ".dlt"
    )
    @KafkaListener(
        topics = KafkaTopics.EXTRACTION_STARTED_EVENTS,
        groupId = KafkaGroups.WEBSITE_EXTRACTION_GROUP,
        concurrency = "1"  // m7i-flex.large (2 vCPU): 최초 시작 이벤트, 빈도 낮음
    )
    public void handleExtractionStartedEvent(
        @Payload ExtractionStartedEvent event,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
        @Header(KafkaHeaders.OFFSET) Long offset,
        Acknowledgment acknowledgment
    )
    {
        try {
            log.info("웹사이트 추출 시작 - Topic: {}, WebsiteId: {}, MainUrl: {}, Partition: {}, Offset: {}",
                    topic, event.websiteId().getId(), event.mainUrl(), partition, offset);

            // Website 조회
            Website website = getWebsitePort.findById(event.websiteId())
                    .orElseThrow(() -> new IllegalStateException("Website 도메인을 찾을 수 없습니다"));

            // 크롤링 시작 가능 여부 검증 혹시라고 pending이 아니라 시작중인 url일 수 있으니
            if (!website.canStartCrawling()) {
                throw new IllegalStateException("크롤링을 시작할 수 없는 상태입니다: " + website.getExtractionStatus());
            }

            // UrlCrawlEvent가 직접 루트 크롤 이벤트 생성
            // 즉 urlCrawlEventConsumer가 실행 처음
            /* 예시
                websiteId: 랜덤 수
                URL: https://example.com
                parentUrl: null
                depth: 0
                eventOccurredAt: 이벤트 발행시간
             */
            UrlCrawlEvent rootCrawlEvent = UrlCrawlEvent.createRootCrawl(
                    website.getWebsiteId(),
                    website.getMainUrl()
            );

            // 상태를 PROGRESS로 변경 (크롤링 시작)
            Website inProgressWebsite = website.startExtraction();
            saveWebsitePort.save(inProgressWebsite);

            eventDispatcher.dispatch(rootCrawlEvent);
            acknowledgment.acknowledge();

            log.info("루트 URL 크롤링 이벤트 발행 완료 - WebsiteId: {}, MaxDepth: {}",
                    event.websiteId().getId(), inProgressWebsite.getCrawlConfig().maxDepth());

        } catch (IllegalArgumentException e) {
            log.error("잘못된 요청 - WebsiteId: {}, MainUrl: {}, Error: {}",
                    event.websiteId().getId(), event.mainUrl(), e.getMessage());
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("웹사이트 추출 실패 (재시도 예정) - WebsiteId: {}, MainUrl: {}, Error: {}",
                    event.websiteId().getId(), event.mainUrl(), e.getMessage(), e);
            throw e; // @RetryableTopic이 재시도 처리
        }
    }
}
