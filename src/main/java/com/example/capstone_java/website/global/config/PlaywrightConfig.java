package com.example.capstone_java.website.global.config;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Playwright 설정 클래스
 *
 * 공유 Playwright와 Browser 인스턴스를 Spring Bean으로 관리하여
 * 애플리케이션 전체에서 재사용합니다.
 *
 * 왜 공유 인스턴스인가?
 * - Playwright와 Browser 생성은 매우 무겁고 시간이 오래 걸립니다.
 * - 매 요청마다 생성/종료하면 서버 성능이 심각하게 저하됩니다.
 * - 대신 가벼운 BrowserContext와 Page는 필요할 때마다 생성/종료합니다.
 */
@Slf4j
@Configuration
public class PlaywrightConfig {

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
     * 공유 브라우저 인스턴스를 Bean으로 등록
     * (애플리케이션 종료 시 자동으로 close)
     *
     * Headless 모드로 실행되며, 서버 환경에 최적화된 옵션을 사용합니다.
     */
    @Bean(destroyMethod = "close")
    public Browser browser(Playwright playwright) {
        log.info("Headless Browser Bean 생성 중...");
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
            .setHeadless(true) // 백엔드 실행이므로 'headless' 모드 필수
            .setArgs(List.of(
                "--disable-gpu", // GPU 가속 비활성화 (서버 환경 권장)
                "--no-sandbox",  // 샌드박스 비활성화 (Docker/Linux 환경에서 필요할 수 있음)
                "--disable-dev-shm-usage" // /dev/shm 메모리 사용 비활성화
            ));

        // Chromium 브라우저를 사용
        return playwright.chromium().launch(options);
    }
}
