package com.example.capstone_java.website.application.event.handler;

import com.example.capstone_java.website.application.port.out.JsoupPort;
import com.example.capstone_java.website.application.port.out.SaveCrawledUrlPort;
import com.example.capstone_java.website.domain.entity.CrawledUrl;
import com.example.capstone_java.website.domain.event.DiscoveredUrlsEvent;
import com.example.capstone_java.website.domain.event.DomainEvent;
import com.example.capstone_java.website.domain.event.EventHandler;
import com.example.capstone_java.website.domain.event.UrlCrawlEvent;
import com.example.capstone_java.website.application.event.EventDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UrlCrawlEventProcessingHandler implements EventHandler<UrlCrawlEvent> {

    private final JsoupPort jsoupPort;
    private final EventDispatcher eventDispatcher;
    private final SaveCrawledUrlPort saveCrawledUrlPort;

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof UrlCrawlEvent;
    }

    @Override
    public void handle(UrlCrawlEvent event) {
        try {
            log.info("URL 크롤링 처리 시작: WebsiteId={}, URL={}, Depth={}/{}",
                    event.websiteId().getId(), event.url(), event.depth(), event.maxDepth());

            if (event.hasReachedMaxDepth()) {
                log.info("최대 크롤링 깊이 도달. 크롤링 중단: URL={}, Depth={}", event.url(), event.depth());
                return;
            }

            CrawledUrl crawledUrl = event.isRootUrl()
                ? CrawledUrl.rootUrl(event.websiteId(), event.url())
                : CrawledUrl.discovered(event.websiteId(), event.url(), event.parentUrl(), event.depth());

            Set<String> discoveredUrls = jsoupPort.getCrawledUrls(event.url());
            log.info("발견된 URL 개수: {}", discoveredUrls.size());

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

            CrawledUrl completedUrl = crawledUrl.markCrawled();
            saveCrawledUrlPort.save(completedUrl);
            log.info("URL 크롤링 완료: URL={}, Depth={}, 발견된 URL 수={}",
                    event.url(), event.depth(), discoveredUrls.size());

        } catch (Exception e) {
            log.error("URL 크롤링 실패: WebsiteId={}, URL={}, Error={}",
                    event.websiteId().getId(), event.url(), e.getMessage(), e);

            CrawledUrl failedUrl = CrawledUrl.discovered(event.websiteId(), event.url(), event.parentUrl(), event.depth())
                    .markFailed();
            saveCrawledUrlPort.save(failedUrl);
        }
    }

    private boolean shouldCrawlUrl(String url, String parentUrl) {
        if (url.equals(parentUrl)) {
            return false;
        }
        return true;
    }
}