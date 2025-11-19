package com.example.capstone_java.website.adapter.in.kafka;

import com.example.capstone_java.website.adapter.in.dto.AiAnalysisResponse;
import com.example.capstone_java.website.application.port.out.GetWebsitePort;
import com.example.capstone_java.website.application.port.out.SaveAccessibilityReportPort;
import com.example.capstone_java.website.domain.entity.AccessibilityReport;
import com.example.capstone_java.website.domain.entity.Website;
import com.example.capstone_java.website.domain.vo.WebsiteId;
import com.example.capstone_java.website.global.common.KafkaGroups;
import com.example.capstone_java.website.global.common.KafkaTopics;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * AI 분석 결과를 최종 저장하는 Kafka 컨슈머
 *
 * 주요 책임:
 * 1. AnalysisCallbackController가 발행한 AI 분석 결과 수신
 * 2. 거대한 JSON 결과를 도메인 객체로 변환
 * 3. AccessibilityReport로 DB에 저장
 * 4. 트랜잭션으로 데이터 일관성 보장
 *
 * 병렬 처리:
 * - concurrency = "3": EC2 Large 최적화 (DB 쓰기)
 * - DB 저장 작업이므로 적절한 동시성 유지
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisResultConsumer {

    private final SaveAccessibilityReportPort saveAccessibilityReportPort;
    private final GetWebsitePort getWebsitePort;
    private final ObjectMapper objectMapper;

    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 2000, multiplier = 2.0),
        dltTopicSuffix = ".dlt"
    )
    @KafkaListener(
        topics = KafkaTopics.ACCESSIBILITY_JUDGED_EVENTS,
        groupId = KafkaGroups.AI_JUDGMENT_GROUP,
        concurrency = "3"  // m7i-flex.large: DB 저장 I/O
    )
    @Transactional
    public void handleAnalysisResult(
        @Payload Map<String, Object> analysisResult,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
        @Header(KafkaHeaders.OFFSET) Long offset,
        Acknowledgment acknowledgment
    ) {
        try {
            log.info("AI 분석 결과 처리 시작 - Topic: {}, Partition: {}, Offset: {}", topic, partition, offset);

            // Map을 DTO로 변환
            AiAnalysisResponse aiResponse = objectMapper.convertValue(analysisResult, AiAnalysisResponse.class);

            // DTO에서 필수 정보 추출
            String url = aiResponse.getUrl();
            String taskId = aiResponse.getTaskId();
            String websiteIdStr = aiResponse.getWebsiteId();

            log.info("AI 분석 완료 - WebsiteId: {}, URL: {}, TaskId: {}, Score: {}",
                    websiteIdStr, url, taskId, aiResponse.getAccessibilityScore());

            if (websiteIdStr == null || websiteIdStr.isEmpty()) {
                log.error("AI 응답에 website_id가 없음 - URL: {}, TaskId: {}", url, taskId);
                acknowledgment.acknowledge();
                return;
            }

            // WebsiteId 도메인 객체 생성
            UUID websiteUuid = UUID.fromString(websiteIdStr);
            WebsiteId websiteId = WebsiteId.of(websiteUuid);

            // Website 존재 여부 확인
            Website website = getWebsitePort.findById(websiteId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "존재하지 않는 WebsiteId: " + websiteIdStr));

            // 도메인 객체 생성 (원본 Map과 DTO 모두 전달)
            AccessibilityReport report = AccessibilityReport.create(
                websiteId,
                url,
                analysisResult,  // 전체 JSON 저장용
                aiResponse,      // 파싱된 DTO
                taskId
            );

            // DB에 저장
            AccessibilityReport savedReport = saveAccessibilityReportPort.save(report);

            log.info("AI 분석 결과 저장 완료 - Report ID: {}, WebsiteId: {}, URL: {}, Score: {}",
                    savedReport.getId(), websiteId.getId(), savedReport.getUrl(), savedReport.getAccessibilityScore());

            // 저장 성공 후 메시지 처리 완료
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("AI 분석 결과 처리 실패 (재시도 예정) - Error: {}", e.getMessage(), e);
            throw e;  // acknowledge 없이 throw → Kafka가 메시지 재시도
        }
    }

}
