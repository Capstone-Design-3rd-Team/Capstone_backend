package com.example.capstone_java.website.application.service;

import com.example.capstone_java.website.application.port.out.GetWebsitePort;
import com.example.capstone_java.website.application.port.out.SaveCrawledUrlPort;
import com.example.capstone_java.website.application.port.out.SaveWebsitePort;
import com.example.capstone_java.website.application.port.out.JsoupPort;
import com.example.capstone_java.website.domain.entity.CrawledUrl;
import com.example.capstone_java.website.domain.entity.CrawlingContext;
import com.example.capstone_java.website.domain.entity.Website;
import com.example.capstone_java.website.domain.event.UrlCrawlEvent;
import com.example.capstone_java.website.application.event.EventDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 객체지향 설계 기반 크롤링 실행 서비스
 * Tell, Don't Ask 원칙에 따라 도메인 객체들에게 책임을 위임하고
 * 서비스는 객체들 간의 협력만 조정
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlExecutionService {

    private final JsoupPort jsoupPort;
    private final GetWebsitePort getWebsitePort;
    private final SaveWebsitePort saveWebsitePort;
    private final SaveCrawledUrlPort saveCrawledUrlPort;
    private final EventDispatcher eventDispatcher;

    /**
     * 객체지향적 크롤링 실행
     * 각 객체가 자신의 책임을 다하도록 협력 조정
     */
    public void executeCrawl(UrlCrawlEvent event) {
        Website website = null;
        try {
            log.info("크롤링 시작: {}", event.url());

            // 1. 도메인 객체 조회 -- 인덱싱을 통한 최적화 필요
            website = getWebsitePort.findById(event.websiteId())
                    .orElseThrow(() -> new IllegalStateException("Website를 찾을 수 없습니다: " + event.websiteId()));

            // 2. maxTotalUrls 제한 체크 -- redis를 통한 최적화
            long currentUrlCount = saveCrawledUrlPort.countByWebsiteId(event.websiteId());
            if (website.hasReachedCrawlLimits((int) currentUrlCount)) {
                log.info("크롤링 중단: maxTotalUrls 제한 도달 - WebsiteId: {}, 현재 URL 수: {}, 최대 URL 수: {}",
                        website.getWebsiteId().getId(), currentUrlCount, website.getCrawlConfig().maxTotalUrls());
                return;
            }

            // 3. 크롤링 컨텍스트 생성 및 자가 검증 (Tell, Don't Ask) -
            // 스스로 깊이 검증을 하는건 좋은데 크롤링 컨텍스트라는 클래스를 만들어야 했을까?
            CrawlingContext context = CrawlingContext.start(website, event)
                    .validateDepth();  // 컨텍스트가 스스로 깊이 검증

            if (!context.shouldProceed()) {
                log.info(context.getSummary());
                return;
            }

            // 4. 외부 의존성 처리 (서비스의 고유 책임) 실제 url 추출
            // jsoup이 아니라 셀리니움 or playwrith 도입 고민
            Set<String> rawUrls = extractUrlsFromExternal(event.url());

            // 5. 컨텍스트가 스스로 URL 처리 (Tell, Don't Ask)
            // 중복 제거, 경로 검증, 페이지당 url 제한 하는 역할
            CrawlingContext processedContext = context.withDiscoveredUrls(List.copyOf(rawUrls));

            // 6. 이벤트 발행 협력 (컨텍스트가 준비되면 서비스가 발행)
            // → DiscoveredUrlsEvent { urls: [page2, page3, page4] } 발행
            // → Kafka로 전송 → JobUpdatingConsumer가 받음
            processedContext.toEventIfReady()
                    .ifPresent(this::publishEvent);

            // 7. 크롤링 완료 처리 및 저장 - db 저장인데 이건 필요할거 같음
            CrawledUrl completedUrl = processedContext.toCrawledUrl();
            saveCrawledUrlPort.save(completedUrl);

            log.info(processedContext.getSummary());
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
     * 외부 의존성 처리 (서비스의 고유 책임)
     */
    private Set<String> extractUrlsFromExternal(String url) {
        return jsoupPort.getCrawledUrls(url);
    }

    /**
     * 이벤트 발행 (서비스의 고유 책임)
     */
    private void publishEvent(com.example.capstone_java.website.domain.event.DiscoveredUrlsEvent event) {
        eventDispatcher.dispatch(event);
    }

    /**
     * 실패 처리 (도메인 메서드 활용)
     */
    private void handleCrawlingFailure(UrlCrawlEvent event, Website website) {
        // CrawledUrl 실패 처리 및 저장
        CrawledUrl failedUrl = CrawledUrl.discovered(event.websiteId(), event.url(), event.parentUrl(), event.depth())
                .markFailed();
        saveCrawledUrlPort.save(failedUrl);

        log.info("URL 크롤링 실패 처리 완료 - WebsiteId: {}, URL: {}",
                website.getWebsiteId().getId(), event.url());
    }
}