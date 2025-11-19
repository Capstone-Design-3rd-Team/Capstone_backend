package com.example.capstone_java.website.global.config;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

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
@EnableScheduling
public class PlaywrightConfig {

    @Value("${playwright.pool.size:4}")
    private int poolSize;

    private static Playwright playwrightInstance;
    private static int poolSizeStatic;

    /**
     * Playwright 메인 인스턴스를 Bean으로 등록
     * (애플리케이션 종료 시 자동으로 close)
     */
    @Bean(destroyMethod = "close")
    public Playwright playwright() {
        log.info("Playwright Bean 생성 중...");
        playwrightInstance = Playwright.create();
        return playwrightInstance;
    }

    /**
     * 브라우저 풀을 Bean으로 등록
     * BlockingQueue를 사용하여 thread-safe한 브라우저 풀 관리
     */
    @Bean
    public BlockingQueue<Browser> browserPool(Playwright playwright) {
        log.info("Playwright 브라우저 풀 생성 중... (크기: {})", poolSize);

        poolSizeStatic = poolSize; // static 변수에 저장
        BlockingQueue<Browser> pool = new ArrayBlockingQueue<>(poolSize);

        for (int i = 0; i < poolSize; i++) {
            Browser browser = createBrowser(playwright);
            pool.offer(browser);
            log.info("브라우저 인스턴스 생성 완료: {}/{}", i + 1, poolSize);
        }

        log.info("브라우저 풀 초기화 완료 (총 {}개)", poolSize);
        return pool;
    }

    /**
     * 애플리케이션 종료 시 브라우저 풀의 모든 인스턴스 정리 및 헬스체크
     */
    @Bean
    public BrowserPoolManager browserPoolManager(BlockingQueue<Browser> browserPool) {
        return new BrowserPoolManager(browserPool, poolSize);
    }

    /**
     * Playwright 전체 재시작 (긴급 복구용)
     */
    public static synchronized void restartPlaywright(BlockingQueue<Browser> browserPool) {
        log.warn("========================================");
        log.warn("Playwright 전체 재시작 시작");
        log.warn("========================================");

        try {
            // 1. 기존 브라우저 모두 종료
            List<Browser> oldBrowsers = new ArrayList<>();
            browserPool.drainTo(oldBrowsers);
            for (Browser browser : oldBrowsers) {
                try {
                    browser.close();
                } catch (Exception e) {
                    log.debug("브라우저 종료 중 오류 무시: {}", e.getMessage());
                }
            }
            log.info("기존 브라우저 {}개 종료 완료", oldBrowsers.size());

            // 2. Playwright 인스턴스 재생성
            try {
                if (playwrightInstance != null) {
                    playwrightInstance.close();
                }
            } catch (Exception e) {
                log.debug("Playwright 종료 중 오류 무시: {}", e.getMessage());
            }

            Thread.sleep(1000); // 1초 대기 (리소스 정리 시간)

            playwrightInstance = Playwright.create();
            log.info("Playwright 인스턴스 재생성 완료");

            // 3. 새 브라우저 풀 생성
            int successCount = 0;
            for (int i = 0; i < poolSizeStatic; i++) {
                try {
                    Browser newBrowser = createBrowser(playwrightInstance);
                    browserPool.offer(newBrowser);
                    successCount++;
                    log.info("새 브라우저 생성 ({}/{})", i + 1, poolSizeStatic);
                    Thread.sleep(500); // 브라우저 생성 간격
                } catch (Exception e) {
                    log.error("재시작 중 브라우저 생성 실패 ({}/{}): {}", i + 1, poolSizeStatic, e.getMessage());
                }
            }

            log.warn("========================================");
            log.warn("Playwright 전체 재시작 완료 - 성공: {}/{}", successCount, poolSizeStatic);
            log.warn("========================================");

        } catch (Exception e) {
            log.error("Playwright 재시작 실패! 애플리케이션 재시작 필요", e);
        }
    }

    /**
     * 브라우저 풀 관리 클래스 (정리 + 헬스체크)
     */
    public static class BrowserPoolManager {
        private final BlockingQueue<Browser> browserPool;
        private final int poolSize;
        private int consecutiveFailures = 0;
        private static final int MAX_CONSECUTIVE_FAILURES = 3;

        public BrowserPoolManager(BlockingQueue<Browser> browserPool, int poolSize) {
            this.browserPool = browserPool;
            this.poolSize = poolSize;
        }

        /**
         * 주기적으로 브라우저 풀 상태 확인 및 손상된 브라우저 교체
         * 5분마다 실행
         */
        @Scheduled(fixedDelay = 300000, initialDelay = 60000) // 5분마다, 첫 실행은 1분 후
        public void healthCheck() {
            log.debug("브라우저 풀 헬스체크 시작 (현재 풀 크기: {})", browserPool.size());

            // 풀이 비어있으면 즉시 Playwright 재시작
            if (browserPool.isEmpty()) {
                log.error("브라우저 풀이 완전히 비어있음! 즉시 Playwright 재시작");
                restartPlaywright(browserPool);
                return;
            }

            List<Browser> healthyBrowsers = new ArrayList<>();
            List<Browser> unhealthyBrowsers = new ArrayList<>();

            // 풀에서 모든 브라우저를 꺼내 상태 확인
            browserPool.drainTo(healthyBrowsers);

            for (Browser browser : healthyBrowsers) {
                try {
                    // isConnected()뿐만 아니라 실제로 브라우저를 테스트
                    if (!isBrowserActuallyHealthy(browser)) {
                        log.warn("헬스체크에서 손상된 브라우저 감지");
                        unhealthyBrowsers.add(browser);
                    }
                } catch (Exception e) {
                    log.warn("브라우저 상태 확인 중 오류: {}", e.getMessage());
                    unhealthyBrowsers.add(browser);
                }
            }

            // 손상된 브라우저 제거
            healthyBrowsers.removeAll(unhealthyBrowsers);

            // 손상된 브라우저 종료
            for (Browser browser : unhealthyBrowsers) {
                try {
                    browser.close();
                } catch (Exception e) {
                    log.warn("손상된 브라우저 종료 중 오류 무시: {}", e.getMessage());
                }
            }

            // 손상된 브라우저 개수만큼 새로 생성
            if (!unhealthyBrowsers.isEmpty()) {
                log.info("{}개의 손상된 브라우저 교체 중...", unhealthyBrowsers.size());

                int successCount = 0;
                for (int i = 0; i < unhealthyBrowsers.size(); i++) {
                    try {
                        Browser newBrowser = createBrowser(playwrightInstance);
                        healthyBrowsers.add(newBrowser);
                        successCount++;
                        log.info("새 브라우저 생성 완료 ({}/{})", i + 1, unhealthyBrowsers.size());
                    } catch (Exception e) {
                        log.error("새 브라우저 생성 실패: {}", e.getMessage());
                        consecutiveFailures++;
                    }
                }

                // 연속 실패 감지 시 Playwright 전체 재시작
                if (successCount == 0 && consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                    log.error("연속 {}회 브라우저 생성 실패! Playwright 전체 재시작 시도", consecutiveFailures);
                    consecutiveFailures = 0;
                    restartPlaywright(browserPool);
                    return; // 재시작 후 종료
                }

                if (successCount > 0) {
                    consecutiveFailures = 0; // 성공하면 카운터 리셋
                }
            }

            // 모든 건강한 브라우저를 풀에 다시 추가
            for (Browser browser : healthyBrowsers) {
                browserPool.offer(browser);
            }

            log.info("브라우저 풀 헬스체크 완료 (정상: {}, 교체: {}, 연속실패: {})",
                    healthyBrowsers.size(), unhealthyBrowsers.size(), consecutiveFailures);
        }

        /**
         * 브라우저가 실제로 사용 가능한지 테스트
         * isConnected()만으로는 내부 객체 손상을 감지 못하므로 실제 동작 확인
         */
        private boolean isBrowserActuallyHealthy(Browser browser) {
            try {
                // 1. 연결 상태 확인
                if (!browser.isConnected()) {
                    return false;
                }

                // 2. 실제로 Context 생성이 가능한지 테스트
                BrowserContext testContext = browser.newContext();
                try {
                    testContext.close();
                } catch (Exception e) {
                    log.debug("BrowserContext 테스트 실패: {}", e.getMessage());
                    return false;
                }

                return true;
            } catch (Exception e) {
                log.debug("브라우저 헬스체크 실패: {}", e.getMessage());
                return false;
            }
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

    /**
     * 브라우저 인스턴스 생성 (공통 로직)
     */
    private static Browser createBrowser(Playwright playwright) {
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setArgs(List.of(
                        "--disable-gpu",
                        "--no-sandbox",
                        "--disable-dev-shm-usage",
                        "--disable-setuid-sandbox",
                        "--disable-software-rasterizer",
                        "--disable-extensions"
                ));

        return playwright.chromium().launch(options);
    }
}