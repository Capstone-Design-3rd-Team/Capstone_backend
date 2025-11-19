package com.example.capstone_java.website.adapter.in.kafka;

import com.example.capstone_java.website.application.service.CrawlExecutionService;
import com.example.capstone_java.website.domain.event.UrlCrawlEvent;
import com.example.capstone_java.website.global.common.KafkaGroups;
import com.example.capstone_java.website.global.common.KafkaTopics;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.TimeoutError;
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
public class UrlCrawlEventConsumer {

    private final CrawlExecutionService crawlExecutionService;

    @RetryableTopic(
            attempts = "1",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltTopicSuffix = ".dlt",
            // [중요] 아래 예외들이 발생하면 재시도하지 말고 즉시 포기(DLT로 직행)해라!
            exclude = {
                    PlaywrightException.class, // Playwright 관련 모든 에러 (브라우저 크래시, 객체 손상 등)
                    TimeoutError.class, // Playwright 타임아웃 (느린 페이지, 무응답 등)
                    IllegalArgumentException.class, // 잘못된 URL 등
                    NullPointerException.class // 코드 버그 등
            }
    )
    @KafkaListener(
        topics = KafkaTopics.URL_CRAWL_EVENTS,
        groupId = KafkaGroups.URL_PROCESSING_GROUP,
        concurrency = "4"  // Playwright 브라우저 풀 크기와 동일
    )
    public void handleUrlCrawlEvent(
        @Payload UrlCrawlEvent event,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
        @Header(KafkaHeaders.OFFSET) Long offset,
        Acknowledgment acknowledgment
    ) {
        try {
            log.info("URL 크롤링 이벤트 수신 - Topic: {}, WebsiteId: {}, URL: {}, Depth: {}, Partition: {}, Offset: {}",
                    topic, event.websiteId().getId(), event.url(), event.depth(), partition, offset);

            // 도메인 로직 중심의 크롤링 실행
            crawlExecutionService.executeCrawl(event);

            // 수동 커밋
            acknowledgment.acknowledge();

            log.debug("URL 크롤링 이벤트 처리 완료 - URL: {}, Depth: {}", event.url(), event.depth());

        } catch (IllegalArgumentException e) {
            log.error("잘못된 URL 크롤링 요청 - WebsiteId: {}, URL: {}, Error: {}",
                    event.websiteId().getId(), event.url(), e.getMessage());
            // 잘못된 요청은 재시도하지 않고 바로 커밋
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("URL 크롤링 처리 실패 (재시도 예정) - WebsiteId: {}, URL: {}, Depth: {}, Error: {}",
                    event.websiteId().getId(), event.url(), event.depth(), e.getMessage(), e);
            // @RetryableTopic이 재시도 처리
            throw e;
        }
    }
}