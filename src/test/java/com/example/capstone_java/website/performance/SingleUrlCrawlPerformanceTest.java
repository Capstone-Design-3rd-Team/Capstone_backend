package com.example.capstone_java.website.performance;

import com.example.capstone_java.website.adapter.out.JsoupAdapter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;

/**
 * 단일 URL 크롤링 성능 테스트
 *
 * 목적: 실제 웹사이트 1개 크롤링하는데 걸리는 시간 측정
 */
@Slf4j
@SpringBootTest
public class SingleUrlCrawlPerformanceTest {

    @Autowired(required = false)
    private JsoupAdapter jsoupAdapter;

    private static final String TEST_URL = "https://www.kbanknow.com/ib20/mnu/PBKMAN000000";

    @Test
    public void testSingleUrlCrawlTime() {
        if (jsoupAdapter == null) {
            log.warn("JsoupAdapter not available, skipping test");
            return;
        }

        log.info("===========================================");
        log.info("단일 URL 크롤링 성능 테스트");
        log.info("===========================================");
        log.info("테스트 URL: {}", TEST_URL);
        log.info("");

        long startTime = System.currentTimeMillis();

        try {
            Set<String> discoveredUrls = jsoupAdapter.getCrawledUrls(TEST_URL);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            log.info("✓ 크롤링 완료!");
            log.info("");
            log.info("--- 결과 ---");
            log.info("발견된 URL 수: {}개", discoveredUrls.size());
            log.info("소요 시간: {}ms", duration);
            log.info("소요 시간: {}초", String.format("%.2f", duration / 1000.0));
            log.info("");

            // 발견된 URL 일부 출력
            log.info("발견된 URL 샘플 (최대 10개):");
            discoveredUrls.stream()
                .limit(10)
                .forEach(url -> log.info("  - {}", url));

            if (discoveredUrls.size() > 10) {
                log.info("  ... 외 {}개", discoveredUrls.size() - 10);
            }

            log.info("===========================================");

        } catch (Exception e) {
            log.error("✗ 크롤링 실패: {}", e.getMessage(), e);
        }
    }
}
