/*
package com.example.capstone_java.website.adapter.out;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

*/
/**
 * PlaywrightStrategy 통합 테스트
 *
 * 실제 Playwright 브라우저를 사용하여 URL 추출을 테스트합니다.
 *//*

@Slf4j
class PlaywrightStrategyTest {

    private static Playwright playwright;
    private static Browser browser;
    private static PlaywrightStrategy playwrightStrategy;

    @BeforeAll
    static void setUp() {
        log.info("Playwright 테스트 환경 초기화 중...");

        // Playwright와 Browser 인스턴스 생성 (실제 환경과 동일)
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
            .setHeadless(true)
            .setArgs(List.of(
                "--disable-gpu",
                "--no-sandbox",
                "--disable-dev-shm-usage"
            )));

        // PlaywrightStrategy 인스턴스 생성
        playwrightStrategy = new PlaywrightStrategy(browser);

        log.info("Playwright 테스트 환경 초기화 완료");
    }

    @AfterAll
    static void tearDown() {
        log.info("Playwright 테스트 환경 정리 중...");
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
        log.info("Playwright 테스트 환경 정리 완료");
    }

    @Test
    void 정적_HTML_페이지에서_URL_추출_성공() {
        // Given: 간단한 정적 HTML 페이지
        String url = "https://example.com";

        // When: URL 추출
        List<String> urls = playwrightStrategy.extractUrls(url);

        // Then: URL이 추출되어야 함
        assertNotNull(urls, "추출된 URL 리스트는 null이 아니어야 합니다");
        log.info("추출된 URL 개수: {}", urls.size());

        if (!urls.isEmpty()) {
            log.info("추출된 URL 샘플 (최대 5개):");
            urls.stream().limit(5).forEach(extractedUrl -> log.info("  - {}", extractedUrl));
        }
    }

    @Test
    void GitHub_페이지에서_URL_추출_성공() {
        // Given: GitHub 메인 페이지 (JavaScript가 많이 사용되는 페이지)
        //String url = "https://github.com";
        String url = "https://www.kbanknow.com/ib20/mnu/PBKMAN000000#";

        // When: URL 추출
        List<String> urls = playwrightStrategy.extractUrls(url);

        // Then: URL이 추출되어야 함
        assertNotNull(urls, "추출된 URL 리스트는 null이 아니어야 합니다");
        assertFalse(urls.isEmpty(), "GitHub 페이지에서 최소 1개 이상의 URL이 추출되어야 합니다");

        log.info("test에서 추출된 URL 개수: {}", urls.size());
        log.info("추출된 URL 샘플 (최대 10개):");
        urls.stream().limit(30).forEach(extractedUrl -> log.info("  - {}", extractedUrl));
    }

    @Test
    void 잘못된_URL로_요청시_빈_리스트_반환() {
        // Given: 존재하지 않는 URL
        String invalidUrl = "https://this-domain-does-not-exist-12345.com";

        // When: URL 추출 시도
        List<String> urls = playwrightStrategy.extractUrls(invalidUrl);

        // Then: 빈 리스트가 반환되어야 함 (예외를 던지지 않음)
        assertNotNull(urls, "추출된 URL 리스트는 null이 아니어야 합니다");
        assertTrue(urls.isEmpty(), "잘못된 URL에 대해서는 빈 리스트가 반환되어야 합니다");

        log.info("잘못된 URL 처리 완료: 빈 리스트 반환");
    }

    @Test
    void 링크가_없는_페이지_처리() {
        // Given: 간단한 텍스트만 있는 페이지
        String url = "https://example.com";

        // When: URL 추출
        List<String> urls = playwrightStrategy.extractUrls(url);

        // Then: 빈 리스트이거나 URL이 있을 수 있음 (example.com에는 IANA 링크가 있을 수 있음)
        assertNotNull(urls, "추출된 URL 리스트는 null이 아니어야 합니다");

        log.info("링크가 적은 페이지 처리 완료: {} URLs", urls.size());
    }

    @Test
    void getName_메서드_테스트() {
        // When & Then
        assertEquals("Playwright", playwrightStrategy.getName());
    }

    @Test
    void getPriority_메서드_테스트() {
        // When & Then
        assertEquals(2, playwrightStrategy.getPriority(),
            "Playwright는 Jsoup보다 나중에 시도되어야 하므로 우선순위가 2여야 합니다");
    }

    @Test
    void canHandle_메서드_테스트() {
        // When & Then
        assertFalse(playwrightStrategy.canHandle("https://example.com"),
            "현재 구현에서는 수동 선택이므로 false를 반환해야 합니다");
    }
}
*/
