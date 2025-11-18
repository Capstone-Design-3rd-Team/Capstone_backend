package com.example.capstone_java.website.adapter.out;

import com.example.capstone_java.website.application.port.out.CrawlStrategy;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
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

    @Override
    public List<String> extractUrls(String url) {
        int maxRetries = 3;
        long baseDelay = 500; // 0.5초

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return doExtractUrls(url);

            } catch (PlaywrightException e) {
                if (attempt == maxRetries) {
                    log.error("Playwright 크롤링 최종 실패 - URL: {}, 재시도 {}/{}회 모두 실패, Error: {}",
                             url, maxRetries, maxRetries, e.getMessage());
                    return List.of();
                }

                long delay = baseDelay * attempt; // 0.5초, 1초, 1.5초
                log.warn("Playwright 일시적 실패 - URL: {}, 재시도 {}/{}, {}ms 후 재시도, Error: {}",
                         url, attempt, maxRetries, delay, e.getMessage());

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("재시도 대기 중 인터럽트 발생 - URL: {}", url);
                    return List.of();
                }
            }
        }
        return List.of();
    }

    /**
     * 실제 URL 추출 로직 (재시도 대상)
     */
    private List<String> doExtractUrls(String url) {
        Set<String> uniqueUrls = new LinkedHashSet<>();
        String baseUrl = extractBaseUrl(url);

        Browser browser = null;
        try {
            // 1. 브라우저 풀에서 브라우저 가져오기 (없으면 대기)
            browser = browserPool.take();
            log.debug("브라우저 풀에서 브라우저 획득 (남은 개수: {})", browserPool.size());

            // 2. BrowserContext와 Page는 가벼우므로 매번 생성/종료
            try (BrowserContext context = browser.newContext();
                 Page page = context.newPage()) {

                // 3. 페이지 타임아웃 설정 (무한 대기 방지)
                page.setDefaultTimeout(15_000); // 15초

                log.debug("Playwright로 네비게이션 시작: {}", url);

                // 4. 페이지로 이동 (Playwright는 기본적으로 'load' 이벤트까지 자동 대기)
                page.navigate(url);

                // SPA 사이트가 완전히 로드되도록 추가 대기
                page.waitForTimeout(2000); // 2초 대기

                log.debug("Playwright로 DOM 분석 시작: {}", url);

            // 4. JavaScript를 사용하여 모든 링크와 클릭 가능한 요소에서 URL 추출
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

                log.info("Playwright가 {}에서 {}개의 URL 추출", url, uniqueUrls.size());
            }

            return new ArrayList<>(uniqueUrls);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("브라우저 풀에서 브라우저 획득 중 인터럽트 발생", e);
            return List.of();
        } catch (PlaywrightException e) {
            // 재시도를 위해 예외를 다시 던짐
            throw e;
        } finally {
            // 6. 반드시 브라우저를 풀에 반환
            if (browser != null) {
                browserPool.offer(browser);
                log.debug("브라우저 풀에 반환 완료 (현재 개수: {})", browserPool.size());
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
