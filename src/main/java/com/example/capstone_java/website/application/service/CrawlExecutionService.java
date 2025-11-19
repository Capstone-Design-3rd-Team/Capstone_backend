package com.example.capstone_java.website.application.service;

import com.example.capstone_java.website.adapter.out.PlaywrightStrategy;
import com.example.capstone_java.website.application.port.out.GetWebsitePort;
import com.example.capstone_java.website.application.port.out.SaveCrawledUrlPort;
import com.example.capstone_java.website.application.port.out.SaveWebsitePort;
import com.example.capstone_java.website.domain.entity.CrawledUrl;
import com.example.capstone_java.website.domain.entity.Website;
import com.example.capstone_java.website.domain.event.DiscoveredUrlsEvent;
import com.example.capstone_java.website.domain.event.UrlAnalysisRequestEvent;
import com.example.capstone_java.website.domain.event.UrlCrawlEvent;
import com.example.capstone_java.website.application.event.EventDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 객체지향 설계 기반 크롤링 실행 서비스
 * 도메인 객체에게 책임을 위임하고 서비스는 객체들 간의 협력만 조정
 *
 * 크롤링 전략: Playwright 단독 사용
 * - JavaScript 실행 후 동적 콘텐츠 추출
 * - onclick 이벤트의 viewGo, goMenu 등 사용자 정의 함수 처리
 * - 정적 <a href> 태그도 모두 추출
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlExecutionService {

    private final PlaywrightStrategy playwrightStrategy;
    private final GetWebsitePort getWebsitePort;
    private final SaveWebsitePort saveWebsitePort;
    private final SaveCrawledUrlPort saveCrawledUrlPort;
    private final EventDispatcher eventDispatcher;

    @Value("${app.callback.base-url:http://localhost:8080}")
    private String callbackBaseUrl;

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

            // 3. URL 추출: Playwright로 JavaScript 실행 후 URL 추출
            List<String> rawUrls = extractUrls(event.url());

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

            // 7. AI 분석 요청 이벤트 발행 (크롤링된 URL 자체에 대한 분석 요청)
            String callbackUrl = callbackBaseUrl + "/api/analysis/callback";
            UrlAnalysisRequestEvent analysisEvent = UrlAnalysisRequestEvent.create(
                event.websiteId(),
                event.url(),
                callbackUrl,
                event.depth()
            );
            eventDispatcher.dispatch(analysisEvent);
            log.info("AI 분석 요청 이벤트 발행 완료 - URL: {}", event.url());

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
     * Playwright로 URL 추출
     *
     * 동작 방식:
     * 1. Playwright가 브라우저로 페이지 로드
     * 2. JavaScript 실행 후 DOM에서 URL 추출
     * 3. <a href>, onclick 이벤트, button 속성 등 모두 수집
     * 4. viewGo(), goMenu() 등 사용자 정의 JavaScript 함수를 실제 URL로 변환
     *
     * 장점:
     * - 정적 링크 + 동적 링크 모두 추출
     * - JavaScript 기반 SPA 사이트 완벽 지원
     * - onclick 이벤트의 사용자 정의 함수 처리
     */
    private List<String> extractUrls(String url) {
        List<String> urls = playwrightStrategy.extractUrls(url);

        if (!urls.isEmpty()) {
            log.info("Playwright로 URL 추출 성공: {} URLs", urls.size());
        } else {
            log.warn("URL 추출 실패: {}", url);
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