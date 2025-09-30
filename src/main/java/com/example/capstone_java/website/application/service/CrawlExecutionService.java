package com.example.capstone_java.website.application.service;

import com.example.capstone_java.website.application.port.out.GetWebsitePort;
import com.example.capstone_java.website.application.port.out.JsoupPort;
import com.example.capstone_java.website.domain.entity.CrawledUrl;
import com.example.capstone_java.website.domain.entity.Website;
import com.example.capstone_java.website.domain.event.DiscoveredUrlsEvent;
import com.example.capstone_java.website.domain.event.UrlCrawlEvent;
import com.example.capstone_java.website.application.event.EventDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 실제 크롤링 실행만 담당하는 단일 책임 서비스
 * - URL에서 링크 추출
 * - 도메인 로직 활용한 URL 검증
 * - 발견된 URL 이벤트 발행
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlExecutionService {

    private final JsoupPort jsoupPort;
    private final GetWebsitePort getWebsitePort;
    private final EventDispatcher eventDispatcher;

    /**
     * URL 크롤링 이벤트 처리 - 진짜 DDD 방식 (도메인이 비즈니스 로직 담당)
     */
    public void executeCrawl(UrlCrawlEvent event) {
        try {
            log.info("URL 크롤링 실행 시작: {}", event.url());

            // 1. Website 도메인 조회
            Website website = getWebsitePort.getById(event.websiteId());

            // 2. 도메인이 직접 크롤링 가능 여부 판단
            if (!website.canCrawlAtDepth(event.depth())) {
                log.info("크롤링 중단: {}", website.getCrawlingSummary());
                return;
            }

            // 3. 외부 의존성 - 실제 URL에서 링크 추출 (도메인에서 할 수 없는 작업)
            Set<String> discoveredUrls = jsoupPort.getCrawledUrls(event.url());
            List<String> urlList = List.copyOf(discoveredUrls);

            // 4. 도메인이 직접 URL 필터링 및 검증
            List<String> validUrls = website.filterValidUrls(urlList);
            List<String> finalUrls = website.excludeParentUrl(validUrls, event.url());

            // 5. 도메인 객체 생성 및 상태 변경
            CrawledUrl crawledUrl = event.isRootUrl()
                ? CrawledUrl.rootUrl(event.websiteId(), event.url())
                : CrawledUrl.discovered(event.websiteId(), event.url(), event.parentUrl(), event.depth());

            // 6. 유효한 URL이 있으면 이벤트 발행 (인프라 작업)
            if (!finalUrls.isEmpty()) {
                DiscoveredUrlsEvent discoveredEvent = DiscoveredUrlsEvent.create(
                    event.websiteId(), event.url(), finalUrls, event.depth(), event.maxDepth()
                );
                eventDispatcher.dispatch(discoveredEvent);
            }

            // 7. 도메인 상태 변경
            CrawledUrl completedUrl = crawledUrl.markCrawled();
            log.info("크롤링 완료: URL={}, 유효 URL 수={}", event.url(), finalUrls.size());

        } catch (Exception e) {
            log.error("크롤링 실패: {}", event.url(), e);
            // 실패 처리는 도메인 메서드 사용
            CrawledUrl.discovered(event.websiteId(), event.url(), event.parentUrl(), event.depth())
                    .markFailed();
            throw e;
        }
    }
}