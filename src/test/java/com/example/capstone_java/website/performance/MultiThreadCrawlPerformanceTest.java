package com.example.capstone_java.website.performance;

import com.example.capstone_java.website.adapter.out.JsoupAdapter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 멀티 스레드 크롤링 성능 비교 테스트
 *
 * 목적: 스레드 개수에 따른 처리 시간 비교
 */
@Slf4j
@SpringBootTest
public class MultiThreadCrawlPerformanceTest {

    @Autowired(required = false)
    private JsoupAdapter jsoupAdapter;

    private static final String BASE_URL = "https://www.kbanknow.com/ib20/mnu/PBKMAN000000";

    @Test
    public void testMultiThreadPerformance() throws InterruptedException {
        if (jsoupAdapter == null) {
            log.warn("JsoupAdapter not available, skipping test");
            return;
        }

        log.info("===========================================");
        log.info("멀티 스레드 크롤링 성능 비교 테스트");
        log.info("===========================================");
        log.info("기본 URL: {}", BASE_URL);
        log.info("");

        // 먼저 실제 URL들을 크롤링해서 가져옴
        log.info("1단계: 테스트용 URL 수집 중...");
        Set<String> urls = jsoupAdapter.getCrawledUrls(BASE_URL);
        List<String> testUrls = new ArrayList<>(urls);

        // 최대 20개만 테스트 (너무 많으면 시간이 오래 걸림)
        if (testUrls.size() > 20) {
            testUrls = testUrls.subList(0, 20);
        }

        log.info("✓ 테스트용 URL {}개 수집 완료", testUrls.size());
        log.info("");

        // 다양한 스레드 개수로 테스트
        int[] threadCounts = {1, 2, 4, 8};

        log.info("2단계: 스레드 개수별 성능 측정");
        log.info("-------------------------------------------");

        for (int threadCount : threadCounts) {
            testWithThreadCount(testUrls, threadCount);
            log.info("");
        }

        log.info("===========================================");
    }

    private void testWithThreadCount(List<String> urls, int threadCount) throws InterruptedException {
        log.info("[{}개 스레드 테스트]", threadCount);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicInteger totalUrlsFound = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        List<Future<?>> futures = new ArrayList<>();

        for (String url : urls) {
            Future<?> future = executor.submit(() -> {
                try {
                    Set<String> crawledUrls = jsoupAdapter.getCrawledUrls(url);
                    successCount.incrementAndGet();
                    totalUrlsFound.addAndGet(crawledUrls.size());
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.debug("크롤링 실패: {} - {}", url, e.getMessage());
                }
            });
            futures.add(future);
        }

        // 모든 작업 완료 대기
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                log.error("작업 실행 중 오류", e);
            }
        }

        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.MINUTES);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // 결과 출력
        log.info("  총 URL 수: {}개", urls.size());
        log.info("  성공: {}개, 실패: {}개", successCount.get(), failCount.get());
        log.info("  발견된 총 URL: {}개", totalUrlsFound.get());
        log.info("  총 소요 시간: {}ms ({}초)", duration, String.format("%.2f", duration / 1000.0));
        log.info("  평균 처리 시간: {}ms/URL", duration / urls.size());

        if (duration > 0) {
            double urlsPerSecond = (double) urls.size() / (duration / 1000.0);
            log.info("  처리 속도: {} URL/초", String.format("%.2f", urlsPerSecond));
        }
    }
}
