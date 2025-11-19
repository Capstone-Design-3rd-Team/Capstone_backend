package com.example.capstone_java.website.adapter.out;

import com.example.capstone_java.website.application.port.out.RequestAiAnalysisPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Flask AI 서버에 접근성 분석을 요청하는 어댑터
 *
 * Flask AI 서버 API:
 * - URL: http://43.200.175.194:8000/analyze
 * - Method: POST
 * - Request Body: {"url": "...", "callback_url": "...", "website_id": "..."}
 * - Response: {"status": "processing", "task_id": "...", "message": "..."}
 */
@Slf4j
@Component
public class AiAnalysisAdapter implements RequestAiAnalysisPort {

    private final RestTemplate restTemplate;
    private final String aiServerUrl;

    public AiAnalysisAdapter(
            RestTemplate restTemplate,
            @Value("${ai.server.url:http://43.200.175.194:8000}") String aiServerUrl
    ) {
        this.restTemplate = restTemplate;
        this.aiServerUrl = aiServerUrl;
    }

    @Override
    public String requestAnalysis(String websiteId, String url, String callbackUrl) {
        log.info("AI 분석 요청 시작 - WebsiteId: {}, URL: {}, Callback: {}", websiteId, url, callbackUrl);

        try {
            // 요청 본문 생성
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("url", url);
            requestBody.put("callback_url", callbackUrl);
            requestBody.put("website_id", websiteId);

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            // AI 서버에 POST 요청
            String analyzeEndpoint = aiServerUrl + "/analyze";
            ResponseEntity<Map> response = restTemplate.exchange(
                    analyzeEndpoint,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            // 응답 확인
            if (response.getStatusCode() == HttpStatus.ACCEPTED || response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                String taskId = responseBody != null ? (String) responseBody.get("task_id") : "unknown";
                log.info("AI 분석 요청 성공 - Task ID: {}, WebsiteId: {}, URL: {}", taskId, websiteId, url);
                return taskId;
            } else {
                log.error("AI 분석 요청 실패 - Status: {}, WebsiteId: {}, URL: {}", response.getStatusCode(), websiteId, url);
                throw new AiServerException("AI 서버 비정상 응답: " + response.getStatusCode(), true);
            }

        } catch (org.springframework.web.client.ResourceAccessException e) {
            // 네트워크 연결 오류, 타임아웃 등 (재시도 가능)
            log.error("AI 서버 연결 실패 (재시도 가능) - WebsiteId: {}, URL: {}, Error: {}",
                    websiteId, url, e.getMessage());
            throw new AiServerException("AI 서버 연결 실패: " + e.getMessage(), true, e);

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // 4xx 클라이언트 오류 (재시도 불가능)
            log.error("AI 요청 형식 오류 (재시도 불가) - WebsiteId: {}, URL: {}, Status: {}, Response: {}",
                    websiteId, url, e.getStatusCode(), e.getResponseBodyAsString());
            throw new AiServerException("잘못된 요청 형식: " + e.getMessage(), false, e);

        } catch (org.springframework.web.client.HttpServerErrorException e) {
            // 5xx 서버 오류 (재시도 가능)
            log.error("AI 서버 내부 오류 (재시도 가능) - WebsiteId: {}, URL: {}, Status: {}, Response: {}",
                    websiteId, url, e.getStatusCode(), e.getResponseBodyAsString());
            throw new AiServerException("AI 서버 내부 오류: " + e.getMessage(), true, e);

        } catch (AiServerException e) {
            // 이미 처리된 예외는 그대로 전파
            throw e;

        } catch (Exception e) {
            // 기타 예상치 못한 오류 (재시도 가능)
            log.error("AI 분석 요청 중 예상치 못한 오류 - WebsiteId: {}, URL: {}, Error: {}",
                    websiteId, url, e.getMessage(), e);
            throw new AiServerException("예상치 못한 오류: " + e.getMessage(), true, e);
        }
    }

    /**
     * AI 서버 관련 커스텀 예외
     */
    public static class AiServerException extends RuntimeException {
        private final boolean retryable;

        public AiServerException(String message, boolean retryable) {
            super(message);
            this.retryable = retryable;
        }

        public AiServerException(String message, boolean retryable, Throwable cause) {
            super(message, cause);
            this.retryable = retryable;
        }

        public boolean isRetryable() {
            return retryable;
        }
    }
}
