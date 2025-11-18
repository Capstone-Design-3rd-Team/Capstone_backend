package com.example.capstone_java.website.global.config;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Playwright 설정 클래스
 *
 * 브라우저 풀을 사용하여 동시성 처리와 리소스 효율성을 모두 확보합니다.
 *
 * 브라우저 풀 패턴:
 * - 고정된 개수의 Browser 인스턴스를 미리 생성
 * - BlockingQueue로 관리하여 thread-safe 보장
 * - 사용 후 풀에 반환하여 재사용
 * - 동시 처리 개수 = 풀 크기로 자연스럽게 제한
 */
@Slf4j
@Configuration
public class PlaywrightConfig {

    @Value("${playwright.pool.size:4}")
    private int poolSize;

    /**
     * Playwright 메인 인스턴스를 Bean으로 등록
     * (애플리케이션 종료 시 자동으로 close)
     */
    @Bean(destroyMethod = "close")
    public Playwright playwright() {
        log.info("Playwright Bean 생성 중...");
        return Playwright.create();
    }

    /**
     * 브라우저 풀을 Bean으로 등록
     * BlockingQueue를 사용하여 thread-safe한 브라우저 풀 관리
     */
    @Bean
    public BlockingQueue<Browser> browserPool(Playwright playwright) {
        log.info("Playwright 브라우저 풀 생성 중... (크기: {})", poolSize);

        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
            .setHeadless(true)
            .setArgs(List.of(
                "--disable-gpu",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-setuid-sandbox"
            ));

        BlockingQueue<Browser> pool = new ArrayBlockingQueue<>(poolSize);

        for (int i = 0; i < poolSize; i++) {
            Browser browser = playwright.chromium().launch(options);
            pool.offer(browser);
            log.info("브라우저 인스턴스 생성 완료: {}/{}", i + 1, poolSize);
        }

        log.info("브라우저 풀 초기화 완료 (총 {}개)", poolSize);
        return pool;
    }

    /**
     * 애플리케이션 종료 시 브라우저 풀의 모든 인스턴스 정리
     */
    @Bean
    public BrowserPoolCleaner browserPoolCleaner(BlockingQueue<Browser> browserPool) {
        return new BrowserPoolCleaner(browserPool);
    }

    /**
     * 브라우저 풀 정리 클래스
     */
    public static class BrowserPoolCleaner {
        private final BlockingQueue<Browser> browserPool;

        public BrowserPoolCleaner(BlockingQueue<Browser> browserPool) {
            this.browserPool = browserPool;
        }

        @jakarta.annotation.PreDestroy
        public void cleanup() {
            log.info("브라우저 풀 정리 중...");
            List<Browser> browsers = new ArrayList<>();
            browserPool.drainTo(browsers);

            for (Browser browser : browsers) {
                try {
                    browser.close();
                } catch (Exception e) {
                    log.error("브라우저 종료 중 오류 발생", e);
                }
            }
            log.info("브라우저 풀 정리 완료 ({}개 종료)", browsers.size());
        }
    }
}
