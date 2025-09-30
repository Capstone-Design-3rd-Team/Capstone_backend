package com.example.capstone_java.website.adapter.in.kafka;

import com.example.capstone_java.website.application.port.out.GetWebsitePort;
import com.example.capstone_java.website.domain.entity.Website;
import com.example.capstone_java.website.domain.event.ExtractionStartedEvent;
import com.example.capstone_java.website.domain.event.UrlCrawlEvent;
import com.example.capstone_java.website.global.common.KafkaFactories;
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
    private final EventDispatcher eventDispatcher;

    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 2.0),
        dltTopicSuffix = ".dlt"
    )
    @KafkaListener(
        topics = KafkaTopics.EXTRACTION_STARTED_EVENTS,
        groupId = KafkaGroups.WEBSITE_EXTRACTION_GROUP,
        containerFactory = KafkaFactories.EXTRACTION_LISTENER_CONTAINER_FACTORY
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

            // Website에서 크롤링 설정 조회
            Website website = getWebsitePort.findById(event.websiteId())
                    .orElseThrow(() -> new IllegalStateException("Website 도메인을 찾을 수 없습니다"));

            // 도메인 로직으로 크롤링 가능 여부 확인
            if (!website.canStartCrawling()) {
                log.warn("크롤링을 시작할 수 없는 상태 - WebsiteId: {}, Status: {}",
                        event.websiteId().getId(), website.getExtractionStatus());
                acknowledgment.acknowledge();
                return;
            }

            // 도메인 메서드로 루트 크롤링 이벤트 생성
            UrlCrawlEvent rootCrawlEvent = website.createRootCrawlEvent();

            eventDispatcher.dispatch(rootCrawlEvent);
            acknowledgment.acknowledge();

            log.info("루트 URL 크롤링 이벤트 발행 완료 - WebsiteId: {}, MaxDepth: {}",
                    event.websiteId().getId(), website.getCrawlConfig().maxDepth());

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
