package com.example.capstone_java.website.adapter.out;

import com.example.capstone_java.website.application.port.out.CrawlStrategy;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Playwright 기반 동적 JavaScript 크롤링 전략
 *
 * 책임: JavaScript 실행 후 URL 추출 (검증/필터링은 Website 도메인이 함)
 *
 * 브라우저 풀 패턴:
 * - BlockingQueue에서 브라우저를 빌려옴 (take)
 * - 사용 후 반드시 풀에 반환 (offer)
 * - 풀이 비어있으면 자동으로 대기 (thread-safe)
 * - 동시 처리 개수가 풀 크기로 자연스럽게 제한됨
 *
 * JavaScript 링크 처리:
 * - javascript:goMenu('CODE') 패턴을 실제 URL로 변환
 * - SPA 사이트의 동적 메뉴 링크 추출
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlaywrightStrategy implements CrawlStrategy {

    // PlaywrightConfig에서 생성한 브라우저 풀을 주입받음
    private final BlockingQueue<Browser> browserPool;

    // JavaScript 함수 호출 패턴 (예: javascript:goMenu('HOMBKI030000'))
    private static final Pattern JS_GO_MENU_PATTERN = Pattern.compile("javascript:goMenu\\(['\"]([A-Z0-9]+)['\"]\\)");

    // 메뉴 코드 패턴 (예: HOMBKI030000)
    private static final Pattern MENU_CODE_PATTERN = Pattern.compile("[A-Z]{3,}[A-Z0-9]{6,}");

    // 타임아웃 설정 (밀리초) - EC2 환경 최적화
    private static final int PAGE_LOAD_TIMEOUT_MS = 8_000;   // 8초 (브라우저 풀 고갈 방지)
    private static final int NAVIGATION_TIMEOUT_MS = 10_000;  // 10초 (리다이렉트 고려)
    private static final int DOM_WAIT_TIMEOUT_MS = 1_000;     // 1초 (SPA 로딩 대기)

    // URL 차단 패턴 (소문자로 비교)
    private static final List<String> BLOCKED_URL_PATTERNS = List.of(
            // 인증 관련
            "logout", "signout", "sign-out", "sign_out",
            "login", "signin", "sign-in", "sign_in",
            "auth/", "sso/", "saml/",

            // 프로토콜
            "javascript:", "mailto:", "tel:", "ftp:",

            // LMS 특정 차단 (실제 문제 발생한 URL)
            "total_survey_list_form.acl",  // 문제 발생한 설문 페이지
            "total_survey",                // 설문 관련 전체
            ".acl?.*logout", ".acl?.*login",

            // 파일 다운로드 (크롤링 불필요)
            ".pdf", ".zip", ".hwp", ".xlsx", ".xls", ".ppt", ".pptx",
            ".doc", ".docx", ".txt", ".csv",
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".svg",
            ".mp4", ".avi", ".mov", ".mp3", ".wav",

            // 기타
            "void(0)", "#"
    );

    /**
     * URL이 차단 패턴에 해당하는지 검사
     */
    private boolean shouldBlockUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return true;
        }

        String lowerUrl = url.toLowerCase();
        for (String pattern : BLOCKED_URL_PATTERNS) {
            if (lowerUrl.contains(pattern)) {
                log.debug("🚫 차단된 URL 패턴 매칭: '{}' in {}", pattern, url);
                return true;
            }
        }
        return false;
    }

    /**
     * URL 추출 (절대 예외를 던지지 않음 - 항상 List 반환)
     *
     * 안전 장치:
     * 1. URL 패턴 필터링으로 1차 차단
     * 2. 모든 예외를 catch하여 빈 리스트 반환
     * 3. 손상된 브라우저는 폐기 (풀에 반납 안 함)
     * 4. 풀 복구는 HealthCheck 스케줄러에 맡김
     */
    @Override
    public List<String> extractUrls(String url) {
        // 1차 방어선: URL 필터링
        if (shouldBlockUrl(url)) {
            log.warn("🚫 필터링된 URL (스킵): {}", url);
            return List.of();
        }

        // 2차 방어선: 안전한 크롤링 (절대 예외를 던지지 않음)
        return doExtractUrlsSafe(url);
    }

    /**
     * 안전한 URL 추출 로직 (절대 예외를 던지지 않음)
     */
    private List<String> doExtractUrlsSafe(String url) {
        Browser browser = null;
        boolean browserAcquired = false;

        try {
            // 1. 브라우저 대여
            browser = browserPool.take();
            browserAcquired = true;
            log.debug("🔒 브라우저 획득 - URL: {}, 풀 남은 개수: {}", url, browserPool.size());

            // 2. 브라우저 상태 확인
            if (!isBrowserHealthy(browser)) {
                log.warn("⚠️ 손상된 브라우저 감지 -> 폐기 처분");
                closeBrowserSafely(browser);
                browserAcquired = false; // 반납하지 않음 (폐기)
                return List.of(); // 이번 요청은 실패 처리 (재시도 안 함)
            }

            // 3. 크롤링 수행
            List<String> result = performCrawlingWithBrowser(browser, url);
            log.debug("🔓 브라우저 작업 완료 - URL: {}, 추출 URL: {}개", url, result.size());
            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ 브라우저 획득 중단 (인터럽트) - URL: {}", url);
            return List.of();

        } catch (Exception e) {
            // [핵심] 어떤 에러가 나도 로그만 찍고 빈 리스트 반환 -> Kafka 재시도 방지
            log.error("❌ 크롤링 실패 (URL: {}): {}", url, e.getMessage());

            // 에러가 났다는 건 브라우저가 오염됐을 가능성 높음 -> 폐기 결정
            if (browserAcquired && browser != null) {
                log.warn("⚠️ 에러 발생한 브라우저 폐기 처분");
                closeBrowserSafely(browser);
                browserAcquired = false; // 반납하지 않음
            }

            return List.of(); // 빈 리스트 반환으로 Kafka는 "정상 처리"로 인식

        } finally {
            // 4. 정상적인 브라우저만 반납
            if (browserAcquired && browser != null) {
                try {
                    // 반납 전 한 번 더 체크
                    if (isBrowserHealthy(browser)) {
                        browserPool.offer(browser);
                        log.debug("🔓 브라우저 반납 완료 - 풀 크기: {}", browserPool.size());
                    } else {
                        log.warn("⚠️ 반납 직전 브라우저 손상 감지 -> 폐기");
                        closeBrowserSafely(browser);
                    }
                } catch (Exception e) {
                    log.error("브라우저 반납 중 오류", e);
                }
            }
        }
    }


    /**
     * 브라우저를 독점하여 크롤링 수행
     * 에러 발생 시 예외를 던지며, 호출자(doExtractUrlsSafe)가 처리함
     */
    private List<String> performCrawlingWithBrowser(Browser browser, String url) {
        Set<String> uniqueUrls = new LinkedHashSet<>();
        String baseUrl = extractBaseUrl(url);

        BrowserContext context = null;
        Page page = null;

        try {
            // Context와 Page 생성 (봇 감지 회피 설정)
            context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .setViewportSize(1920, 1080)
            );
            context.setDefaultTimeout(PAGE_LOAD_TIMEOUT_MS);

            page = context.newPage();
            page.setDefaultTimeout(PAGE_LOAD_TIMEOUT_MS);
            page.setDefaultNavigationTimeout(NAVIGATION_TIMEOUT_MS);

            // navigator.webdriver 속성 제거 (가장 핵심적인 봇 감지 회피)
            page.addInitScript("""
                Object.defineProperty(navigator, 'webdriver', {
                    get: () => undefined
                });
            """);

            log.debug("Playwright 네비게이션 시작: {}", url);

            // 페이지로 이동 (에러 발생 시 예외 던짐)
            Response response = page.navigate(url, new Page.NavigateOptions()
                    .setTimeout(NAVIGATION_TIMEOUT_MS)
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

            // HTTP 상태 코드 체크
            if (response != null && response.status() >= 400) {
                log.warn("⚠️ HTTP 에러 응답 - Status: {}, URL: {}", response.status(), url);
                return List.of(); // HTTP 에러는 빈 리스트 반환
            }

            // SPA 로딩 대기
            page.waitForTimeout(DOM_WAIT_TIMEOUT_MS);

            log.debug("Playwright DOM 분석 시작: {}", url);

            // JavaScript로 URL 추출
            Object result = page.evaluate("""
                () => {
                    const urls = new Set();

                    // 1. <a> 태그의 href 속성
                    document.querySelectorAll('a[href]').forEach(link => {
                        const href = link.getAttribute('href');
                        if (href && href.trim() !== '') {
                            urls.add(href);
                        }
                    });

                    // 2. onclick 속성이 있는 모든 요소
                    document.querySelectorAll('[onclick]').forEach(element => {
                        const onclick = element.getAttribute('onclick');
                        if (onclick) {
                            urls.add(onclick);
                        }
                    });

                    // 3. button 요소의 모든 속성
                    document.querySelectorAll('button, [role="button"]').forEach(btn => {
                        // href 속성
                        const href = btn.getAttribute('href');
                        if (href) urls.add(href);

                        // onclick 속성
                        const onclick = btn.getAttribute('onclick');
                        if (onclick) urls.add(onclick);

                        // data-url, data-link 등의 속성
                        for (let i = 0; i < btn.attributes.length; i++) {
                            const attr = btn.attributes[i];
                            const value = attr.value;
                            if (value && (value.startsWith('http') ||
                                         value.startsWith('/') ||
                                         value.startsWith('javascript:'))) {
                                urls.add(value);
                            }
                        }
                    });

                    return Array.from(urls);
                }
                """);

            // 5. 추출된 링크 처리
            if (result instanceof List<?>) {
                for (Object item : (List<?>) result) {
                    if (item instanceof String) {
                        String href = (String) item;
                        String processedUrl = processUrl(href, baseUrl);
                        if (processedUrl != null) {
                            uniqueUrls.add(processedUrl);
                        }
                    }
                }
            }

            log.info("✅ Playwright가 {}에서 {}개의 URL 추출", url, uniqueUrls.size());
            return new ArrayList<>(uniqueUrls);

        } finally {
            // ========================================
            // 무조건 Context와 Page 정리 (브라우저를 깨끗한 상태로)
            // ========================================
            closePageSafely(page);
            closeContextSafely(context);
        }
    }

    /**
     * Page 안전 종료
     */
    private void closePageSafely(Page page) {
        if (page != null) {
            try {
                if (!page.isClosed()) {
                    page.close();
                }
            } catch (Exception e) {
                log.debug("Page 종료 중 오류 무시: {}", e.getMessage());
            }
        }
    }

    /**
     * BrowserContext 안전 종료
     */
    private void closeContextSafely(BrowserContext context) {
        if (context != null) {
            try {
                context.close();
            } catch (Exception e) {
                log.debug("BrowserContext 종료 중 오류 무시: {}", e.getMessage());
            }
        }
    }


    /**
     * 브라우저 상태 확인
     */
    private boolean isBrowserHealthy(Browser browser) {
        try {
            // isConnected()로 브라우저 프로세스가 살아있는지 확인
            return browser.isConnected();
        } catch (Exception e) {
            log.warn("브라우저 상태 확인 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 안전하게 브라우저 종료
     */
    private void closeBrowserSafely(Browser browser) {
        if (browser != null) {
            try {
                if (browser.isConnected()) {
                    browser.close();
                } else {
                    log.debug("브라우저가 이미 연결 해제됨, close() 스킵");
                }
            } catch (Exception e) {
                log.warn("브라우저 종료 중 오류 무시: {}", e.getMessage());
            }
        }
    }


    /**
     * URL 처리: JavaScript 링크를 실제 URL로 변환
     *
     * @param href 원본 href 값
     * @param baseUrl 기본 URL (예: https://www.kbanknow.com)
     * @return 처리된 URL 또는 null
     */
    private String processUrl(String href, String baseUrl) {
        if (href == null || href.trim().isEmpty()) {
            return null;
        }

        // 1. javascript:goMenu('CODE') 패턴 처리
        Matcher jsGoMenuMatcher = JS_GO_MENU_PATTERN.matcher(href);
        if (jsGoMenuMatcher.find()) {
            String menuCode = jsGoMenuMatcher.group(1);
            String convertedUrl = baseUrl + "/ib20/mnu/" + menuCode;
            log.debug("JavaScript 링크 변환: {} -> {}", href, convertedUrl);
            return convertedUrl;
        }

        // 2. onclick 속성에서 메뉴 코드 추출 (예: "goMenu('CODE')")
        if (href.contains("goMenu")) {
            Pattern goMenuPattern = Pattern.compile("goMenu\\(['\"]([A-Z0-9]+)['\"]\\)");
            Matcher matcher = goMenuPattern.matcher(href);
            if (matcher.find()) {
                String menuCode = matcher.group(1);
                String convertedUrl = baseUrl + "/ib20/mnu/" + menuCode;
                log.debug("goMenu 함수 변환: {} -> {}", href, convertedUrl);
                return convertedUrl;
            }
        }

        // 3. onclick 속성에서 viewGo 함수 추출 (예: "viewGo('34')")
        if (href.contains("viewGo")) {
            Pattern viewGoPattern = Pattern.compile("viewGo\\(['\"]([0-9]+)['\"]\\)");
            Matcher matcher = viewGoPattern.matcher(href);
            if (matcher.find()) {
                String topicSeq = matcher.group(1);
                String convertedUrl = baseUrl + "/ilos/guide/guide_topic_form.acl?TOPIC_SEQ=" + topicSeq + "&FLAG=0";
                log.debug("viewGo 함수 변환: {} -> {}", href, convertedUrl);
                return convertedUrl;
            }
        }

        // 4. 일반 URL 처리
        if (href.startsWith("javascript:") || href.startsWith("mailto:") || href.startsWith("tel:") || href.startsWith("#")) {
            return null; // 무시
        }

        // 5. 절대 URL
        if (href.startsWith("http://") || href.startsWith("https://")) {
            return href;
        }

        // 6. 상대 경로를 절대 경로로 변환
        if (href.startsWith("/")) {
            return baseUrl + href;
        }

        return null;
    }

    /**
     * URL에서 기본 URL 추출 (프로토콜 + 도메인)
     *
     * @param url 전체 URL
     */
    private String extractBaseUrl(String url) {
        try {
            URI uri = new URI(url);
            return uri.getScheme() + "://" + uri.getHost();
        } catch (URISyntaxException e) {
            log.warn("URL 파싱 실패: {}", url);
            return "";
        }
    }

    @Override
    public boolean canHandle(String url) {
        // 현재는 사용 안 함 (CrawlExecutionService에서 수동으로 선택)
        return false;
    }

    @Override
    public int getPriority() {
        return 2;  // Jsoup보다 나중에 시도
    }

    @Override
    public String getName() {
        return "Playwright";
    }
}