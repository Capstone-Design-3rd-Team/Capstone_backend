package com.example.capstone_java.website.application.service;

import com.example.capstone_java.website.application.port.in.usecase.CrawlUrlsUseCase;
import com.example.capstone_java.website.application.port.out.JsoupPort;
import com.example.capstone_java.website.domain.entity.CrawledUrl;
import com.example.capstone_java.website.domain.event.DiscoveredUrlsEvent;
import com.example.capstone_java.website.domain.event.UrlCrawlEvent;
import com.example.capstone_java.website.domain.vo.WebsiteId;
import com.example.capstone_java.website.application.event.EventDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlCrawlingService implements CrawlUrlsUseCase {

    private final JsoupPort jsoupPort;
    private final EventDispatcher eventDispatcher;

    @Value("${app.crawling.max-depth:3}")
    private int maxDepth;

    @Override
    public void startCrawling(WebsiteId websiteId, String mainUrl) {
        log.info("루트 URL 크롤링 시작: WebsiteId={}, URL={}", websiteId.getId(), mainUrl);

        // 루트 URL 크롤링 이벤트 발행
        UrlCrawlEvent rootCrawlEvent = UrlCrawlEvent.createRootCrawl(websiteId, mainUrl, maxDepth);
        eventDispatcher.dispatch(rootCrawlEvent);

        log.info("루트 URL 크롤링 이벤트 발행 완료: {}", mainUrl);
    }

    public void processCrawlEvent(UrlCrawlEvent event) {
        try {
            log.info("URL 크롤링 처리 시작: WebsiteId={}, URL={}, Depth={}/{}",
                    event.websiteId().getId(), event.url(), event.depth(), event.maxDepth());

            // 최대 깊이 확인
            if (event.hasReachedMaxDepth()) {
                log.info("최대 크롤링 깊이 도달. 크롤링 중단: URL={}, Depth={}", event.url(), event.depth());
                return;
            }

            // CrawledUrl 도메인 객체 생성
            CrawledUrl crawledUrl = event.isRootUrl()
                ? CrawledUrl.rootUrl(event.websiteId(), event.url())
                : CrawledUrl.discovered(event.websiteId(), event.url(), event.parentUrl(), event.depth());

            // URL에서 링크 추출
            Set<String> discoveredUrls = jsoupPort.getCrawledUrls(event.url());
            log.info("발견된 URL 개수: {}", discoveredUrls.size());

            // 유효한 URL들을 필터링하여 배치 이벤트로 발행
            List<String> validUrls = discoveredUrls.stream()
                .filter(url -> shouldCrawlUrl(url, event.url()))
                .collect(Collectors.toList());

            if (!validUrls.isEmpty()) {
                DiscoveredUrlsEvent discoveredEvent = DiscoveredUrlsEvent.create(
                    event.websiteId(),
                    event.url(),
                    validUrls,
                    event.depth(),
                    event.maxDepth()
                );

                eventDispatcher.dispatch(discoveredEvent);
                log.info("발견된 URL 배치 이벤트 발행: ParentURL={}, 발견된 URL 수={}, Depth={}",
                        event.url(), validUrls.size(), event.depth());
            }

            // 크롤링 완료 처리
            CrawledUrl completedUrl = crawledUrl.markCrawled();
            log.info("URL 크롤링 완료: URL={}, Depth={}, 발견된 URL 수={}",
                    event.url(), event.depth(), discoveredUrls.size());

        } catch (Exception e) {
            log.error("URL 크롤링 실패: WebsiteId={}, URL={}, Error={}",
                    event.websiteId().getId(), event.url(), e.getMessage(), e);

            // 실패한 URL 처리
            CrawledUrl failedUrl = CrawledUrl.discovered(event.websiteId(), event.url(), event.parentUrl(), event.depth())
                    .markFailed();
        }
    }

    private boolean shouldCrawlUrl(String url, String parentUrl) {
        // 동일한 URL 중복 방지 (간단한 구현)
        if (url.equals(parentUrl)) {
            return false;
        }

        // TODO: 향후 개선사항
        // - 이미 크롤링한 URL인지 데이터베이스에서 확인
        // - 도메인 제한 확인
        // - 로봇 배제 표준(robots.txt) 확인

        return true;
    }
}
