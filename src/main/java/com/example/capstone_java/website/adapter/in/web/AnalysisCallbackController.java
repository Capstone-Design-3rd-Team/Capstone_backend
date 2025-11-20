package com.example.capstone_java.website.adapter.in.web;

import com.example.capstone_java.website.adapter.in.dto.AiAnalysisResponse;
import com.example.capstone_java.website.global.common.KafkaTopics;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 서버로부터 분석 결과 콜백을 받는 컨트롤러
 *
 * AI 서버 콜백 형식:
 * - URL: POST http://OUR_SERVER/api/analysis/callback
 * - Body: JSON 형태의 분석 결과 (거대한 데이터)
 * - Files: detection_result.png (스크린샷)
 *
 * 처리 방식:
 * 1. 콜백 수신 즉시 200 OK 응답 (AI 서버에게 "받았다" 통보)
 * 2. 받은 결과를 Kafka에 발행 (무거운 처리는 컨슈머가 담당)
 * 3. 컨트롤러는 빠르게 응답하여 AI 서버와의 연결 유지
 */
@Slf4j
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisCallbackController {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * AI 서버로부터 분석 결과를 받는 콜백 엔드포인트
     *
     * @param analysisResult AI가 보낸 분석 결과 JSON
     * @return 200 OK 응답 (AI 서버에게 수신 확인)
     */
    @PostMapping("/callback")
    public ResponseEntity<Map<String, String>> receiveAnalysisResult(@RequestBody Map<String, Object> analysisResult) {
        try {
            log.info("AI 분석 결과 콜백 수신 - 데이터 크기: {} bytes", analysisResult.toString().length());

            // 결과에서 URL 추출 (파티션 키로 사용)
            String url = extractUrl(analysisResult);
            log.info("AI 분석 완료된 URL: {}", url);

            // Kafka에 결과 발행 (무거운 처리는 컨슈머가 담당)
            kafkaTemplate.send(KafkaTopics.ACCESSIBILITY_JUDGED_EVENTS, url, analysisResult);

            log.info("AI 분석 결과를 Kafka에 발행 완료 - URL: {}", url);

            // AI 서버에게 즉시 응답 (빠른 응답으로 연결 유지)
            return ResponseEntity.ok(Map.of(
                "status", "received",
                "message", "Analysis result received and queued for processing"
            ));

        } catch (Exception e) {
            log.error("AI 콜백 처리 중 오류 발생: {}", e.getMessage(), e);


            // 오류 발생 시 500 에러 반환 → AI 서버가 재시도하도록 유도
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "Error processing callback: " + e.getMessage()
            ));
        }
    }

    /**
     * AI 분석 결과 JSON에서 URL 추출
     * 실제 AI 응답 구조: {"results": {"analysis_info": {"url": "..."}}}
     */
    private String extractUrl(Map<String, Object> analysisResult) {
        try {
            // Map을 DTO로 변환하여 URL 추출
            AiAnalysisResponse aiResponse = objectMapper.convertValue(analysisResult, AiAnalysisResponse.class);
            String url = aiResponse.getUrl();

            if (url != null && !url.isEmpty()) {
                return url;
            }

            // URL을 찾을 수 없는 경우 전체 데이터의 해시값 사용 (파티션 키용)
            log.warn("AI 응답에서 URL을 찾을 수 없음. 해시값 사용");
            return String.valueOf(analysisResult.hashCode());

        } catch (Exception e) {
            log.error("URL 추출 중 오류 발생: {}", e.getMessage());
            return String.valueOf(analysisResult.hashCode());
        }
    }

    /**
     * 헬스 체크 엔드포인트 (AI 서버가 콜백 URL 유효성 확인용)
     */
    @GetMapping("/callback/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "analysis-callback"
        ));
    }
}
