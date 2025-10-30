package com.example.capstone_java.website.application.service;

import com.example.capstone_java.website.adapter.out.JsoupStrategy;
import com.example.capstone_java.website.adapter.out.PlaywrightStrategy;
import com.example.capstone_java.website.application.port.out.GetWebsitePort;
import com.example.capstone_java.website.application.port.out.SaveCrawledUrlPort;
import com.example.capstone_java.website.application.port.out.SaveWebsitePort;
import com.example.capstone_java.website.domain.entity.CrawledUrl;
import com.example.capstone_java.website.domain.entity.Website;
import com.example.capstone_java.website.domain.event.DiscoveredUrlsEvent;
import com.example.capstone_java.website.domain.event.UrlCrawlEvent;
import com.example.capstone_java.website.application.event.EventDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 객체지향 설계 기반 크롤링 실행 서비스
 * 도메인 객체에게 책임을 위임하고 서비스는 객체들 간의 협력만 조정
 *
 * 크롤링 전략: Jsoup(빠름) → Playwright(느림) Fallback
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlExecutionService {

    private static final int MIN_URL_THRESHOLD = 5;  // Jsoup이 찾은 URL이 이 값보다 적으면 Playwright로 재시도

    private final JsoupStrategy jsoupStrategy;
    private final PlaywrightStrategy playwrightStrategy;
    private final GetWebsitePort getWebsitePort;
    private final SaveWebsitePort saveWebsitePort;
    private final SaveCrawledUrlPort saveCrawledUrlPort;
    private final EventDispatcher eventDispatcher;

    public void executeCrawl(UrlCrawlEvent event) {
        Website website = null;
        try {
            log.info("크롤링 시작: {}", event.url());

            // 1. 도메인 객체 조회 -- 인덱싱을 통한 최적화 필요
            website = getWebsitePort.findById(event.websiteId())
                    .orElseThrow(() -> new IllegalStateException("Website를 찾을 수 없습니다: " + event.websiteId()));

            // 2. 깊이 검증
            if (!website.canCrawlAtDepth(event.depth())) {
                log.info("크롤링 스킵: 최대 크롤링 깊이 도달 - URL: {}, Depth: {}", event.url(), event.depth());
                return;
            }

            // 3. URL 추출: Jsoup 먼저 시도, 실패 시 Playwright로 자동 fallback
            List<String> rawUrls = extractUrlsWithFallback(event.url());

            // 4. Website가 직접 URL 필터링 (중복 제거, 경로 검증, 페이지당 url 제한)
            List<String> validUrls = website.filterValidUrls(rawUrls);
            List<String> finalUrls = website.excludeParentUrl(validUrls, event.url());

            // 5. 이벤트 발행 협력
            // → DiscoveredUrlsEvent { urls: [page2, page3, page4] } 발행
            // → Kafka로 전송 → JobUpdatingConsumer가 받음
            // → JobUpdatingConsumer에서 maxTotalUrls, maxDepth, maxDuration 등 모든 제한 체크
            if (!finalUrls.isEmpty()) {
                DiscoveredUrlsEvent discoveredEvent = DiscoveredUrlsEvent.create(
                    event.websiteId(),
                    event.url(),
                    finalUrls,
                    event.depth()
                );
                eventDispatcher.dispatch(discoveredEvent);
            }

            // 6. 크롤링 완료 처리 및 저장
            CrawledUrl crawledUrl = createCrawledUrl(event);
            saveCrawledUrlPort.save(crawledUrl.markCrawled());

            log.info("크롤링 완료: URL={}, 발견된 URL 수={}", event.url(), finalUrls.size());
            log.info("URL 크롤링 완료 - WebsiteId: {}, URL: {}, Depth: {}",
                    website.getWebsiteId().getId(), event.url(), event.depth());

        } catch (Exception e) {
            log.error("크롤링 실패: {}", event.url(), e);
            if (website != null) {
                handleCrawlingFailure(event, website);
            }
            throw e;
        }
    }

    /**
     * Jsoup으로 먼저 시도하고, 실패하거나 URL을 찾지 못하면 Playwright로 재시도
     *
     * 동작 방식:
     * 1. Jsoup으로 크롤링 (빠름, 정적 HTML)
     * 2. URL이 MIN_URL_THRESHOLD보다 적으면 → Playwright로 재시도 (느림, 동적 JavaScript)
     * 3. 둘 다 실패 → 빈 리스트 반환
     *
     * 개선점: JavaScript 기반 사이트는 Jsoup이 소수의 정적 링크만 찾는 경우가 있음.
     *         이 경우 Playwright로 재시도하여 동적으로 생성된 링크도 수집.
     */
    private List<String> extractUrlsWithFallback(String url) {
        // 1차 시도: Jsoup (빠른 정적 크롤링)
        List<String> urls = jsoupStrategy.extractUrls(url);

        if (!urls.isEmpty() && urls.size() >= MIN_URL_THRESHOLD) {
            log.info("Jsoup으로 URL 추출 성공: {} URLs", urls.size());
            return urls;
        }

        // 2차 시도: Playwright (느린 동적 크롤링)
        if (urls.isEmpty()) {
            log.warn("Jsoup으로 URL을 찾지 못함. Playwright로 재시도: {}", url);
        } else {
            log.warn("Jsoup으로 찾은 URL이 너무 적음 ({} < {}). Playwright로 재시도: {}",
                    urls.size(), MIN_URL_THRESHOLD, url);
        }

        urls = playwrightStrategy.extractUrls(url);

        if (!urls.isEmpty()) {
            log.info("Playwright로 URL 추출 성공: {} URLs", urls.size());
        } else {
            log.error("모든 크롤링 전략 실패: {}", url);
        }

        return urls;
    }

    private CrawledUrl createCrawledUrl(UrlCrawlEvent event) {
        boolean isRootUrl = event.parentUrl() == null && event.depth() == 0;

        return isRootUrl
            ? CrawledUrl.rootUrl(event.websiteId(), event.url())
            : CrawledUrl.discovered(event.websiteId(), event.url(), event.parentUrl(), event.depth());
    }

    private void handleCrawlingFailure(UrlCrawlEvent event, Website website) {
        CrawledUrl failedUrl = CrawledUrl.discovered(event.websiteId(), event.url(), event.parentUrl(), event.depth())
                .markFailed();
        saveCrawledUrlPort.save(failedUrl);

        log.info("URL 크롤링 실패 처리 완료 - WebsiteId: {}, URL: {}",
                website.getWebsiteId().getId(), event.url());
    }
}