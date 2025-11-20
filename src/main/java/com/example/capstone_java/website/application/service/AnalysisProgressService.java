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

            // 3. 모든 분석 완료 체크
            if (totalAnalyzed >= totalCrawled && totalCrawled > 0) {
                log.info("모든 분석 완료! - clientId={}, total={}", clientId, totalCrawled);
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
    private void sendAnalyzingProgress(String clientId, int analyzedCount, int totalCount) {
        if (totalCount == 0) return;

        int currentPercentage = (int) ((analyzedCount / (double) totalCount) * 100);
        int lastPercentage = lastSentPercentage.getOrDefault(clientId, 0);

        // 10% 변화가 있을 때만 전송
        if (currentPercentage - lastPercentage >= 10) {
            SseProgressDto progress = SseProgressDto.builder()
                    .stage("ANALYZING")
                    .crawledCount(totalCount)
                    .analyzedCount(analyzedCount)
                    .totalCount(totalCount)
                    .percentage(currentPercentage)
                    .message("AI 분석 중... (" + analyzedCount + "/" + totalCount + ")")
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
            // 1. 모든 분석 결과 조회
            List<AccessibilityReport> reports = getAccessibilityReportPort.findAllByWebsiteId(websiteId);

            // 2. 최종 보고서 생성
            FinalReportDto finalReport = reportGenerationService.generateFinalReport(
                    website.getMainUrl(),
                    clientId,
                    reports);

            // 3. Website 상태를 COMPLETE로 변경 및 저장 (SSE 전송 전에 먼저 저장!)
            Website completedWebsite = website.markCompleted();
            saveWebsitePort.save(completedWebsite);

            log.info("Website 상태 COMPLETE로 변경 완료: websiteId={}", websiteId.getId());

            // 4. 완료 진행 상황 전송 (DB 저장 후!)
            SseProgressDto completedProgress = SseProgressDto.builder()
                    .stage("COMPLETED")
                    .percentage(100)
                    .message("분석 완료!")
                    .build();

            sseEmitters.send(clientId, completedProgress, "progress");

            // 5. 최종 보고서 전송
            sseEmitters.send(clientId, finalReport, "complete");

            log.info("최종 보고서 전송 완료: clientId={}, urls={}, score={}",
                    clientId, reports.size(), finalReport.getAverageScore());

            // 6. SSE 연결 종료
            sseEmitters.complete(clientId);
            lastSentPercentage.remove(clientId);

            log.info("SSE 연결 종료: clientId={}", clientId);

        } catch (Exception e) {
            log.error("최종 보고서 생성 실패: clientId={}", clientId, e);

            // 에러 메시지 전송
            SseProgressDto errorProgress = SseProgressDto.builder()
                    .stage("ERROR")
                    .message("보고서 생성 중 오류가 발생했습니다.")
                    .build();

            sseEmitters.send(clientId, errorProgress, "error");
            sseEmitters.complete(clientId);
        }
    }
}
