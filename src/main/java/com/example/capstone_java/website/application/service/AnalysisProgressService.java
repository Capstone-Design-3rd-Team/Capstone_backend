package com.example.capstone_java.website.application.service;

import com.example.capstone_java.website.adapter.in.dto.FinalReportDto;
import com.example.capstone_java.website.adapter.in.dto.SseProgressDto;
import com.example.capstone_java.website.application.port.out.*;
import com.example.capstone_java.website.domain.entity.AccessibilityReport;
import com.example.capstone_java.website.domain.entity.Website;
import com.example.capstone_java.website.domain.event.AnalysisCompletedEvent;
import com.example.capstone_java.website.domain.vo.WebsiteId;
import com.example.capstone_java.website.global.sse.SseEmitters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

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
    private final ReportGenerationService reportGenerationService;
    private final FinalReportSaver finalReportSaver;  // 🔥 별도 클래스로 분리
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
        //  AI 분석 가능한 URL만 카운트 (DISCOVERED + CRAWLED, FAILED 제외)
        long totalCrawled = getCrawledUrlPort.countAnalyzableUrls(websiteId);

        SseProgressDto progress = SseProgressDto.builder()
                .stage("CRAWLING")
                .crawledCount((int) totalCrawled)
                .analyzedCount(0)
                .totalCount(null)
                .percentage(null)
                .message("URL 수집 중... " + totalCrawled + "개 발견")
                .build();

        sseEmitters.send(clientId, progress, "progress");
        log.debug("크롤링 진행 상황 전송: clientId={}, crawledCount={} (FAILED 제외)", clientId, totalCrawled);
    }

    /**
     * 크롤링 완료 → ANALYZING 단계 시작 SSE 전송
     */
    public void notifyCrawlingCompleted(WebsiteId websiteId) {
        Website website = getWebsitePort.findById(websiteId)
                .orElseThrow(() -> new IllegalArgumentException("Website not found: " + websiteId.getId()));

        String clientId = website.getClientId();
        //  AI 분석 가능한 URL만 카운트 (DISCOVERED + CRAWLED, FAILED 제외)
        long totalAnalyzable = getCrawledUrlPort.countAnalyzableUrls(websiteId);

        // ANALYZING 단계 시작 (0% 전송)
        SseProgressDto progress = SseProgressDto.builder()
                .stage("ANALYZING")
                .crawledCount((int) totalAnalyzable)
                .analyzedCount(0)
                .totalCount((int) totalAnalyzable)
                .percentage(0)
                .message("AI 분석 중... (0/" + totalAnalyzable + ")")
                .build();

        sseEmitters.send(clientId, progress, "progress");
        log.info("크롤링 완료 알림 전송: clientId={}, totalAnalyzable={} (FAILED 제외)", clientId, totalAnalyzable);
    }

    /**
     * AI 분석 완료 이벤트 리스너 (트랜잭션 커밋 후 실행)
     *
     * 실행 시점: AccessibilityReport가 DB에 커밋된 직후
     * 장점:
     * 1. count 조회 시 방금 저장한 report가 포함됨 (정확한 진행률 계산)
     * 2. 마지막 URL 분석 완료 시 totalAnalyzed >= totalCrawled 조건이 정확히 작동
     * 3. 100% 완료 체크가 빠지지 않음
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAnalysisCompleted(AnalysisCompletedEvent event) {
        log.debug("트랜잭션 커밋 후 AI 분석 진행 상황 체크 - websiteId={}", event.websiteId().getId());
        notifyAnalysisProgress(event.websiteId());
    }

    /**
     * AI 분석 진행 상황 SSE 전송
     *
     *  주의: 이 메서드는 트랜잭션 커밋 후에 호출되어야 합니다.
     * - AnalysisResultConsumer에서 save() 후 즉시 호출하면 안 됨!
     * - @TransactionalEventListener(phase = AFTER_COMMIT)로 호출되어야 함
     * - 그래야 DB count 조회 시 방금 저장한 report가 포함됨
     */
    private void notifyAnalysisProgress(WebsiteId websiteId) {
        Website website = getWebsitePort.findById(websiteId)
                .orElseThrow(() -> new IllegalArgumentException("Website not found: " + websiteId.getId()));

        String clientId = website.getClientId();
        //  AI 분석 가능한 URL만 카운트 (DISCOVERED + CRAWLED, FAILED 제외)
        long totalAnalyzable = getCrawledUrlPort.countAnalyzableUrls(websiteId);
        long totalAnalyzed = getAccessibilityReportPort.countByWebsiteId(websiteId);

        log.debug("AI 분석 진행 상황: clientId={}, 분석={}/{} (FAILED 제외)", clientId, totalAnalyzed, totalAnalyzable);

        if (totalAnalyzable == 0) return;

        int currentPercentage = (int) ((totalAnalyzed / (double) totalAnalyzable) * 100);
        int lastPercentage = lastSentPercentage.getOrDefault(clientId, 0);

        // 🔥 핵심 변경: 100%일 때는 progress를 보내지 않음! (complete만 보냄)
        if (currentPercentage < 100 && currentPercentage - lastPercentage >= 10) {
            SseProgressDto progress = SseProgressDto.builder()
                    .stage("ANALYZING")
                    .crawledCount((int) totalAnalyzable)
                    .analyzedCount((int) totalAnalyzed)
                    .totalCount((int) totalAnalyzable)
                    .percentage(currentPercentage)
                    .message("AI 분석 중... (" + totalAnalyzed + "/" + totalAnalyzable + ")")
                    .build();

            sseEmitters.send(clientId, progress, "progress");
            lastSentPercentage.put(clientId, currentPercentage);

            log.info("SSE 진행 상황 전송: clientId={}, {}%", clientId, currentPercentage);
        }

        // 모든 분석 완료 체크
        //  트랜잭션 커밋 후라서 totalAnalyzed에 방금 저장한 report가 포함됨!
        //  FAILED URL은 제외하고 DISCOVERED + CRAWLED만 카운트하므로 정확한 100% 체크
        log.info("🔍 완료 체크: totalAnalyzed={}, totalAnalyzable={}, 조건={}",
                totalAnalyzed, totalAnalyzable, totalAnalyzed >= totalAnalyzable);

        if (totalAnalyzed >= totalAnalyzable) {
            log.info("🎉 모든 분석 완료! - clientId={}, total={}", clientId, totalAnalyzable);
            log.info("⏰ [타임스탬프] 분석 완료 시점: {}", System.currentTimeMillis());
            // 100% progress는 보내지 않고, 바로 DB 저장 후 complete만 보냄
            sendFinalReport(clientId, websiteId, website);
        } else {
            log.warn("⚠️ 아직 완료 안 됨: totalAnalyzed={} < totalAnalyzable={}", totalAnalyzed, totalAnalyzable);
        }
    }

    /**
     * 최종 보고서 생성 및 전송 후 SSE 연결 종료
     */
    /**
     * 완료 신호 DTO (가벼운 신호만 전송)
     */
    private record CompletionSignal(String websiteId, String status) {}

    private void sendFinalReport(String clientId, WebsiteId websiteId, Website website) {
        try {
            log.info("📊 최종 보고서 생성 시작 - clientId={}, websiteId={}", clientId, websiteId.getId());

            // 1. 모든 분석 결과 조회
            List<AccessibilityReport> reports = getAccessibilityReportPort.findAllByWebsiteId(websiteId);
            log.info("📄 분석 결과 조회 완료: {} 개", reports.size());

            if (reports.isEmpty()) {
                log.error("❌ AccessibilityReport가 비어있음! websiteId={}", websiteId.getId());
                log.error("❌ DB에서 조회된 report 개수: {}", getAccessibilityReportPort.countByWebsiteId(websiteId));
                return;  // 빈 보고서 저장하지 않음
            }

            // 2. 최종 보고서 생성
            FinalReportDto finalReport = reportGenerationService.generateFinalReport(
                    website.getMainUrl(),
                    clientId,
                    reports);
            log.info("✅ 최종 보고서 생성 완료: averageScore={}", finalReport.getAverageScore());

            // 3. 🔥 핵심: 외부 클래스를 통한 DB 저장 (Self-Invocation 회피)
            //    → Spring 프록시가 정상 작동
            //    → REQUIRES_NEW 트랜잭션이 즉시 커밋됨
            log.info("⏰ [타임스탬프] DB 저장 호출 직전: {}", System.currentTimeMillis());
            finalReportSaver.saveWithNewTransaction(websiteId, finalReport, website);
            log.info("⏰ [타임스탬프] DB 저장 호출 완료 (커밋됨): {}", System.currentTimeMillis());

            // 4. 가벼운 완료 신호만 SSE로 전송 (DB 커밋 완료 후!)
            //    → 이미 DB에 저장되어 있으므로 프론트가 즉시 조회 가능
            CompletionSignal signal = new CompletionSignal(websiteId.getId().toString(), "COMPLETED");
            log.info("⏰ [타임스탬프] SSE 전송 직전: {}", System.currentTimeMillis());
            log.info("📤 complete 신호 전송 시도 - clientId={}, websiteId={}", clientId, websiteId.getId());
            sseEmitters.send(clientId, signal, "complete");
            log.info("⏰ [타임스탬프] SSE 전송 완료: {}", System.currentTimeMillis());

            log.info("🎊 완료 신호 전송 완료: clientId={}, websiteId={}", clientId, websiteId.getId());

            // 5. SSE 연결 종료
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
