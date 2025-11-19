package com.example.capstone_java.website.adapter.in.kafka;

import com.example.capstone_java.website.adapter.out.AiAnalysisAdapter;
import com.example.capstone_java.website.application.port.out.RequestAiAnalysisPort;
import com.example.capstone_java.website.domain.event.UrlAnalysisRequestEvent;
import com.example.capstone_java.website.global.common.KafkaGroups;
import com.example.capstone_java.website.global.common.KafkaTopics;
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

/**
 * AI 분석 요청을 처리하는 Kafka 컨슈머
 *
 * 주요 책임:
 * 1. UrlAnalysisRequestEvent를 수신
 * 2. AI 서버에 분석 요청 전송 (HTTP POST)
 * 3. 즉시 응답(202 ACCEPTED) 확인 후 acknowledge
 * 4. AI의 실제 분석은 백그라운드에서 진행 (콜백으로 결과 수신)
 *
 * 병렬 처리:
 * - concurrency = "5": EC2 Large에 최적화 (HTTP 요청만 하므로 가벼움)
 * - AI 서버는 즉시 202 응답을 주므로 빠른 처리 가능
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiAnalysisConsumer {

    private final RequestAiAnalysisPort requestAiAnalysisPort;

    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 2000, multiplier = 2.0),
        dltTopicSuffix = ".dlt"
    )
    @KafkaListener(
        topics = KafkaTopics.URL_ANALYSIS_REQUEST_EVENTS,
        groupId = KafkaGroups.AI_ANALYSIS_REQUEST_GROUP,
        concurrency = "4"  // m7i-flex.large: 가벼운 HTTP 요청, 빠른 처리
    )
    public void handleAnalysisRequest(
        @Payload UrlAnalysisRequestEvent event,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
        @Header(KafkaHeaders.OFFSET) Long offset,
        Acknowledgment acknowledgment
    ) {
        try {
            log.info("AI 분석 요청 처리 시작 - Topic: {}, WebsiteId: {}, URL: {}, Partition: {}, Offset: {}",
                    topic, event.websiteId().getId(), event.url(), partition, offset);

            // AI 서버에 분석 요청 전송 (즉시 202 ACCEPTED 응답 받음)
            String taskId = requestAiAnalysisPort.requestAnalysis(
                    event.websiteId().getId().toString(),
                    event.url(),
                    event.callbackUrl()
            );

            log.info("AI 분석 요청 완료 - Task ID: {}, URL: {}", taskId, event.url());

            // AI 서버가 202 응답을 주었으므로 메시지 처리 완료
            acknowledgment.acknowledge();

        } catch (AiAnalysisAdapter.AiServerException e) {
            // 재시도 가능 여부에 따라 처리
            if (e.isRetryable()) {
                log.error("AI 분석 요청 실패 (재시도 예정) - WebsiteId: {}, URL: {}, Error: {}",
                        event.websiteId().getId(), event.url(), e.getMessage());
                throw e;  // acknowledge 없이 throw → Kafka가 메시지 재시도
            } else {
                log.error("AI 분석 요청 실패 (재시도 불가) - WebsiteId: {}, URL: {}, Error: {}",
                        event.websiteId().getId(), event.url(), e.getMessage());
                // 재시도해도 소용없는 오류는 acknowledge하고 DLT로 보내지 않음
                acknowledgment.acknowledge();
            }

        } catch (Exception e) {
            log.error("AI 분석 요청 중 예상치 못한 오류 (재시도 예정) - WebsiteId: {}, URL: {}, Error: {}",
                    event.websiteId().getId(), event.url(), e.getMessage(), e);
            throw e;  // acknowledge 없이 throw → Kafka가 메시지 재시도
        }
    }
}
