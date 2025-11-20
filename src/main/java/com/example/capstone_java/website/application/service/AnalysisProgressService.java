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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 분석 진행 상황 추적 및 SSE 전송
 *
 * 책임:
 * 1. 크롤링 진행 상황 전송 (URL 개수)
 * 2. AI 분석 진행 상황 전송 (퍼센트)
 * 3. 완료 시 최종 보고서 생성 및 전송
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
     * 크롤링 진행 상황 SSE 전송 (URL 저장할 때마다 호출)
     */
    public void notifyCrawlingProgress(WebsiteId websiteId) {
        Website website = getWebsitePort.findById(websiteId)
                .orElseThrow(() -> new IllegalArgumentException("Website not found: " + websiteId.getId()));

        String clientId = website.getClientId();
        long totalCrawled = getCrawledUrlPort.countByWebsiteId(websiteId);

        SseProgressDto progress = SseProgressDto.builder()
                .stage("CRAWLING")
                .crawledCount((int) totalCrawled)
                .analyzedCount(0)
                .totalCount(null)
                .percentage(null)
                .message("URL 수집 중... " + totalCrawled + "개 발견")
                .build();

        sseEmitters.send(clientId, progress, "progress");
        log.debug("크롤링 진행 상황 전송: clientId={}, crawledCount={}", clientId, totalCrawled);
    }

    /**
     * 크롤링 완료 → ANALYZING 단계 시작 SSE 전송
     */
    public void notifyCrawlingCompleted(WebsiteId websiteId) {
        Website website = getWebsitePort.findById(websiteId)
                .orElseThrow(() -> new IllegalArgumentException("Website not found: " + websiteId.getId()));

        String clientId = website.getClientId();
        long totalCrawled = getCrawledUrlPort.countByWebsiteId(websiteId);

        // ANALYZING 단계 시작 (0% 전송)
        SseProgressDto progress = SseProgressDto.builder()
                .stage("ANALYZING")
                .crawledCount((int) totalCrawled)
                .analyzedCount(0)
                .totalCount((int) totalCrawled)
                .percentage(0)
                .message("AI 분석 중... (0/" + totalCrawled + ")")
                .build();

        sseEmitters.send(clientId, progress, "progress");
        log.info("크롤링 완료 알림 전송: clientId={}, totalCrawled={}", clientId, totalCrawled);
    }

    /**
     * AI 분석 진행 상황 SSE 전송 (분석 결과 받을 때마다 호출)
     */
    public void notifyAnalysisProgress(WebsiteId websiteId) {
        Website website = getWebsitePort.findById(websiteId)
                .orElseThrow(() -> new IllegalArgumentException("Website not found: " + websiteId.getId()));

        String clientId = website.getClientId();
        long totalCrawled = getCrawledUrlPort.countByWebsiteId(websiteId);
        long totalAnalyzed = getAccessibilityReportPort.countByWebsiteId(websiteId);

        log.debug("AI 분석 진행 상황: clientId={}, 분석={}/{}", clientId, totalAnalyzed, totalCrawled);

        if (totalCrawled == 0) return;

        int currentPercentage = (int) ((totalAnalyzed / (double) totalCrawled) * 100);
        int lastPercentage = lastSentPercentage.getOrDefault(clientId, 0);

        // 10% 변화가 있을 때만 전송
        if (currentPercentage - lastPercentage >= 10) {
            SseProgressDto progress = SseProgressDto.builder()
                    .stage("ANALYZING")
                    .crawledCount((int) totalCrawled)
                    .analyzedCount((int) totalAnalyzed)
                    .totalCount((int) totalCrawled)
                    .percentage(currentPercentage)
                    .message("AI 분석 중... (" + totalAnalyzed + "/" + totalCrawled + ")")
                    .build();

            sseEmitters.send(clientId, progress, "progress");
            lastSentPercentage.put(clientId, currentPercentage);

            log.info("SSE 진행 상황 전송: clientId={}, {}%", clientId, currentPercentage);
        }

        // 모든 분석 완료 체크
        if (totalAnalyzed >= totalCrawled) {
            log.info("모든 분석 완료! - clientId={}, total={}", clientId, totalCrawled);
            sendFinalReport(clientId, websiteId, website);
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

            // 3. Website 상태를 COMPLETE로 변경 및 저장
            Website completedWebsite = website.markCompleted();
            saveWebsitePort.save(completedWebsite);

            log.info("Website 상태 COMPLETE로 변경 완료: websiteId={}", websiteId.getId());

            // 4. 완료 진행 상황 전송
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
