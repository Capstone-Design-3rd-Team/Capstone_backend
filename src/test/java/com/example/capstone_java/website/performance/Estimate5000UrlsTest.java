package com.example.capstone_java.website.performance;

import com.example.capstone_java.website.adapter.out.JsoupAdapter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 5000개 URL 처리 시간 예측 테스트
 *
 * 목적: 샘플 크롤링을 통해 5000개 URL 처리에 걸릴 시간 예측
 */
@Slf4j
@SpringBootTest
public class Estimate5000UrlsTest {

    @Autowired(required = false)
    private JsoupAdapter jsoupAdapter;

    private static final String TEST_URL = "https://www.kbanknow.com/ib20/mnu/PBKMAN000000";
    private static final int SAMPLE_SIZE = 10;
    private static final int TARGET_URLS = 5000;

    @Test
    public void estimate5000UrlsProcessingTime() throws InterruptedException {
        if (jsoupAdapter == null) {
            log.warn("JsoupAdapter not available, skipping test");
            return;
        }

        log.info("===========================================");
        log.info("5000개 URL 처리 시간 예측");
        log.info("===========================================");
        log.info("테스트 URL: {}", TEST_URL);
        log.info("샘플 크롤링 횟수: {}", SAMPLE_SIZE);
        log.info("");

        long totalTime = 0;
        int successCount = 0;

        log.info("샘플 크롤링 시작...");
        log.info("-------------------------------------------");

        for (int i = 0; i < SAMPLE_SIZE; i++) {
            try {
                long start = System.currentTimeMillis();
                jsoupAdapter.getCrawledUrls(TEST_URL);
                long duration = System.currentTimeMillis() - start;

                totalTime += duration;
                successCount++;
                log.info("샘플 {}/{}: {}ms ({}초)",
                    i + 1, SAMPLE_SIZE, duration, String.format("%.2f", duration / 1000.0));

                // 서버 부하 방지를 위한 딜레이
                if (i < SAMPLE_SIZE - 1) {
                    Thread.sleep(500);
                }

            } catch (Exception e) {
                log.error("샘플 {}/{} 실패: {}", i + 1, SAMPLE_SIZE, e.getMessage());
            }
        }

        log.info("-------------------------------------------");
        log.info("");

        if (successCount == 0) {
            log.error("모든 샘플 크롤링 실패. 테스트 중단.");
            return;
        }

        long avgTime = totalTime / successCount;
        log.info("=== 측정 결과 ===");
        log.info("성공한 샘플: {}/{}", successCount, SAMPLE_SIZE);
        log.info("평균 크롤링 시간: {}ms ({}초)", avgTime, String.format("%.2f", avgTime / 1000.0));
        log.info("");

        // 다양한 스레드 개수로 예측
        log.info("=== {}개 URL 예상 처리 시간 ===", TARGET_URLS);
        log.info("(크롤링만, AI 분석 제외)");
        log.info("-------------------------------------------");

        int[] threadCounts = {1, 2, 4, 8, 12, 16, 20, 24};

        for (int threads : threadCounts) {
            // 예상 시간 = (평균 시간 × 총 URL 수) / 스레드 수
            long estimatedTimeMs = (avgTime * TARGET_URLS) / threads;
            long totalSeconds = estimatedTimeMs / 1000;
            long minutes = totalSeconds / 60;
            long seconds = totalSeconds % 60;

            String timeStr;
            if (minutes > 0) {
                timeStr = String.format("%d분 %d초", minutes, seconds);
            } else {
                timeStr = String.format("%d초", seconds);
            }

            log.info("{:2}개 스레드: {} ({:,}ms)",
                threads, timeStr, estimatedTimeMs);

            // 20분 기준선 표시
            if (estimatedTimeMs <= 20 * 60 * 1000) {
                log.info("            ✓ 20분 이내 달성 가능!");
            }
        }

        log.info("-------------------------------------------");
        log.info("");

        // 추가 정보
        log.info("=== 참고 사항 ===");
        log.info("- 위 시간은 순수 크롤링 시간만 포함");
        log.info("- DB 저장, 이벤트 발행 시간 미포함");
        log.info("- 네트워크 상태에 따라 실제 시간은 달라질 수 있음");
        log.info("- AI 분석 시간은 별도 측정 필요");
        log.info("===========================================");
    }
}
