package com.example.capstone_java.website.application.service;

import com.example.capstone_java.website.adapter.in.dto.FinalReportDto;
import com.example.capstone_java.website.adapter.in.dto.SseProgressDto;
import com.example.capstone_java.website.application.port.out.GetAccessibilityReportPort;
import com.example.capstone_java.website.application.port.out.GetCrawledUrlPort;
import com.example.capstone_java.website.application.port.out.GetWebsitePort;
import com.example.capstone_java.website.application.port.out.SaveWebsitePort;
import com.example.capstone_java.website.domain.entity.AccessibilityReport;
import com.example.capstone_java.website.domain.entity.Website;
import com.example.capstone_java.website.domain.vo.WebsiteId;
import com.example.capstone_java.website.global.sse.SseEmitters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 분석 진행 상황 추적 및 SSE 전송
 *
 * 책임:
 * 1. 진행 상황 계산 (크롤링/분석 개수)
 * 2. 10% 단위로 SSE 전송 (배치 전송)
 * 3. 완료 시 최종 보고서 생성 및 전송
 * 4. SSE 연결 종료
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisProgressService {

    private final GetWebsitePort getWebsitePort;
    private final GetCrawledUrlPort getCrawledUrlPort;
    private final GetAccessibilityReportPort getAccessibilityReportPort;
    private final SaveWebsitePort saveWebsitePort;
    private final ReportGenerationService reportGenerationService;
    private final SseEmitters sseEmitters;

    // 마지막 전송 퍼센트 저장 (clientId -> 마지막 전송 퍼센트)
    private final Map<String, Integer> lastSentPercentage = new ConcurrentHashMap<>();

    /**
     * AI 분석 결과 저장 후 진행 상황 체크
     */
    public void checkProgressAndNotify(WebsiteId websiteId) {
        Website website = getWebsitePort.findById(websiteId)
                .orElseThrow(() -> new IllegalArgumentException("Website not found: " + websiteId.getId()));

        String clientId = website.getClientId();

        // 1. 진행 상황 계산
        long totalCrawled = getCrawledUrlPort.countByWebsiteId(websiteId);
        long totalAnalyzed = getAccessibilityReportPort.countByWebsiteId(websiteId);

        // [추가] 크롤링하다가 실패해서 죽어버린 URL 개수 조회
        long totalFailed = getCrawledUrlPort.countFailedByWebsiteId(websiteId);

        // [수정] 처리된 총 개수 = 성공한 AI 분석 + 실패한 크롤링
        long totalProcessed = totalAnalyzed + totalFailed;

        log.debug("진행 상황: clientId={}, 크롤링={}, 분석={}/{}",
                clientId, totalCrawled, totalAnalyzed, totalCrawled);

        // 2. 크롤링이 완료되었는지 확인
        boolean crawlingFinished = website.isCompleted() || website.isFailed();

        if (!crawlingFinished) {
            // 크롤링 단계 (퍼센트 없음, 개수만)
            sendCrawlingProgress(clientId, (int) totalCrawled);
        } else {
            // 분석 단계 (퍼센트 있음)
            sendAnalyzingProgress(clientId, (int) totalAnalyzed, (int) totalCrawled);

            /// [핵심 수정] 넉넉한 오차 범위 적용 (5개)
            // URL이 100개면 95개만 처리돼도 완료!
            // 단, totalProcessed > 0 체크를 통해 "시작하자마자 완료되는 것"은 방지
            long margin = 5;
            long threshold = Math.max(1, totalCrawled - margin); // 최소 1개는 처리되어야 함

            if (totalProcessed >= threshold) {
                log.info("분석 완료 (오차 5개 허용)! - 전체: {}, 처리됨: {}, 남은거: {}",
                        totalCrawled, totalProcessed, (totalCrawled - totalProcessed));

                sendFinalReport(clientId, websiteId, website);
            }
        }
    }

    /**
     * 크롤링 단계 진행 상황 전송 (개수만, 퍼센트 없음)
     */
    private void sendCrawlingProgress(String clientId, int crawledCount) {
        SseProgressDto progress = SseProgressDto.builder()
                .stage("CRAWLING")
                .crawledCount(crawledCount)
                .analyzedCount(0)
                .totalCount(null)
                .percentage(null)
                .message("URL 수집 중... " + crawledCount + "개 발견")
                .build();

        sseEmitters.send(clientId, progress, "progress");
    }

    /**
     * 분석 단계 진행 상황 전송 (10% 단위 배치 전송)
     */
    private void sendAnalyzingProgress(String clientId, int processedCount, int totalCount) {
        if (totalCount == 0) return;

        int currentPercentage = (int) ((processedCount / (double) totalCount) * 100);
        int lastPercentage = lastSentPercentage.getOrDefault(clientId, 0);

        // 10% 변화가 있거나 완료(100%)되었을 때 전송
        if (currentPercentage - lastPercentage >= 10 || currentPercentage == 100) {
            SseProgressDto progress = SseProgressDto.builder()
                    .stage("ANALYZING")
                    .crawledCount(totalCount)
                    .analyzedCount(processedCount)
                    .totalCount(totalCount)
                    .percentage(currentPercentage)
                    .message("AI 분석 중... (" + processedCount + "/" + totalCount + ")")
                    .build();

            sseEmitters.send(clientId, progress, "progress");
            lastSentPercentage.put(clientId, currentPercentage);

            log.info("SSE 진행 상황 전송: clientId={}, {}%", clientId, currentPercentage);
        }
    }

    /**
     * 최종 보고서 생성 및 전송 후 SSE 연결 종료
     */
    private void sendFinalReport(String clientId, WebsiteId websiteId, Website website) {
        try {
            // 1. 성공한 분석 결과만 조회해서 리포트 생성
            List<AccessibilityReport> reports = getAccessibilityReportPort.findAllByWebsiteId(websiteId);

            FinalReportDto finalReport = reportGenerationService.generateFinalReport(
                    website.getMainUrl(),
                    clientId,
                    reports);

            // 2. Website 상태 저장
            Website completedWebsite = website.markCompleted();
            saveWebsitePort.save(completedWebsite);

            // 3. 완료 신호 전송
            SseProgressDto completedProgress = SseProgressDto.builder()
                    .stage("COMPLETED")
                    .percentage(100)
                    .message("분석 완료!")
                    .build();
            sseEmitters.send(clientId, completedProgress, "progress");

            // 4. 최종 리포트 전송
            sseEmitters.send(clientId, finalReport, "complete");

            log.info("최종 보고서 전송 완료: clientId={}", clientId);

            // 5. 연결 종료
            sseEmitters.complete(clientId);
            lastSentPercentage.remove(clientId);

        } catch (Exception e) {
            log.error("최종 보고서 생성 실패: clientId={}", clientId, e);
            SseProgressDto errorProgress = SseProgressDto.builder()
                    .stage("ERROR")
                    .message("보고서 생성 중 오류가 발생했습니다.")
                    .build();
            sseEmitters.send(clientId, errorProgress, "error");
            sseEmitters.complete(clientId);
        }
    }
}
