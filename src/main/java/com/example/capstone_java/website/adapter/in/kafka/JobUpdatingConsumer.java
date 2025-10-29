package com.example.capstone_java.website.adapter.in.kafka;

import com.example.capstone_java.website.application.port.out.CrawlCachePort;
import com.example.capstone_java.website.application.port.out.GetWebsitePort;
import com.example.capstone_java.website.application.port.out.SaveCrawledUrlPort;
import com.example.capstone_java.website.domain.entity.CrawledUrl;
import com.example.capstone_java.website.domain.entity.Website;
import com.example.capstone_java.website.domain.event.DiscoveredUrlsEvent;
import com.example.capstone_java.website.domain.event.UrlCrawlEvent;
import com.example.capstone_java.website.domain.vo.WebsiteId;
import com.example.capstone_java.website.global.common.KafkaGroups;
import com.example.capstone_java.website.global.common.KafkaTopics;
import com.example.capstone_java.website.application.event.EventDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 발견된 URL들을 배치로 처리하여 중복 체크하고 새로운 크롤링 작업을 생성하는 중앙 관제탑 역할
 *
 * 주요 책임:
 * 1. 배치 단위로 발견된 URL들을 수신
 * 2. 데이터베이스에서 중복 URL 체크 (배치 쿼리)
 * 3. 새로운 URL만 DB에 저장하고 크롤링 이벤트 발행
 * 4. 트랜잭션으로 데이터 일관성 보장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobUpdatingConsumer {

    private final SaveCrawledUrlPort saveCrawledUrlPort;
    private final CrawlCachePort crawlCachePort;
    private final GetWebsitePort getWebsitePort;
    private final EventDispatcher eventDispatcher;

    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 2.0),
        dltTopicSuffix = ".dlt"
    )
    @KafkaListener(
        topics = KafkaTopics.URL_DISCOVERED_EVENTS,
        groupId = KafkaGroups.JOB_UPDATING_GROUP,
        concurrency = "3"
    )
    @Transactional
    public void handleDiscoveredUrls(
        @Payload DiscoveredUrlsEvent event,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
        @Header(KafkaHeaders.OFFSET) Long offset,
        Acknowledgment acknowledgment
    ) {
        try {
            log.info("발견된 URL 배치 처리 시작 - Topic: {}, WebsiteId: {}, URL 개수: {}, Depth: {}, Partition: {}, Offset: {}",
                    topic, event.websiteId().getId(), event.urlCount(), event.depth(), partition, offset);

            // Website 도메인 조회
            Website website = getWebsitePort.findById(event.websiteId())
                    .orElseThrow(() -> new IllegalStateException("website 도메인을 찾을 수 없습니다"));

            // 도메인 로직: 깊이 확인
            // jobupdating에서 다시 이벤트를 받아 확인해야 되기 때문에 크롤링하기 전에 먼저 검사해야한다
            // 자식 depth를 체크 : 새로 만들 자식 url들이 크롤링 가능한지 체크
            if (!website.canCrawlAtDepth(event.depth() + 1)) {
                log.info("최대 크롤링 깊이 도달. 처리 중단 - WebsiteId: {}, Depth: {}",
                        event.websiteId().getId(), event.depth());
                return;
            }

            // 도메인 로직: 전체 URL 수 제한 확인 (DB에서 실제 누적 개수 조회)
            long currentTotalUrls = saveCrawledUrlPort.countByWebsiteId(event.websiteId());
            if (website.hasReachedCrawlLimits(currentTotalUrls)) {
                log.info("크롤링 URL 수 제한 도달. 처리 중단 - WebsiteId: {}, 현재 URL 수: {}, 최대: {}",
                        event.websiteId().getId(), currentTotalUrls, website.getCrawlConfig().maxTotalUrls());
                return;
            }

            // 도메인 로직: 크롤링 시간 제한 확인
            Duration elapsed = Duration.between(website.getCreatedAt(), LocalDateTime.now());
            if (elapsed.compareTo(website.getCrawlConfig().maxDuration()) > 0) {
                log.info("크롤링 시간 제한 도달. 처리 중단 - WebsiteId: {}, 경과 시간: {} 분, 최대: {} 분",
                        event.websiteId().getId(), elapsed.toMinutes(), website.getCrawlConfig().maxDuration().toMinutes());
                return;
            }

            // 도메인이 직접 URL 필터링 (진짜 DDD 방식)
            List<String> validUrls = website.filterValidUrls(event.discoveredUrls());

            // 인프라 레이어 작업: DB 중복 체크 (도메인에서 할 수 없는 작업)
            List<String> newUrls = filterNewUrlsFromCache(validUrls, event.websiteId());

            if (newUrls.isEmpty()) {
                log.info("모든 URL이 이미 발견됨. 새로운 작업 없음 - WebsiteId: {}", event.websiteId().getId());
                return;
            }

            // 새로운 CrawledUrl 엔티티들을 배치로 생성 및 저장
            List<CrawledUrl> crawledUrls = newUrls.stream()
                .map(url -> CrawledUrl.discovered(event.websiteId(), url, event.parentUrl(), event.depth() + 1))
                .collect(Collectors.toList());

            saveCrawledUrlPort.saveAll(crawledUrls);
            log.info("새로운 URL {} 개를 DB에 저장 완료", newUrls.size());

            // 크롤링 이벤트들 생성 및 발행
            List<UrlCrawlEvent> crawlEvents = newUrls.stream()
                .map(url -> UrlCrawlEvent.createChildCrawl(
                    event.websiteId(),
                    url,
                    event.parentUrl(),
                    event.depth() + 1
                ))
                .collect(Collectors.toList());

            // 이벤트 발행
            crawlEvents.forEach(eventDispatcher::dispatch);

            log.info("발견된 URL 배치 처리 완료 - WebsiteId: {}, 처리된 새 URL: {}/{}",
                    event.websiteId().getId(), newUrls.size(), event.urlCount());

            // 모든 처리 완료 후 마지막에 한 번만 acknowledge (메시지 처리 완료를 Kafka에 알림)
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("발견된 URL 배치 처리 실패 (재시도 예정) - WebsiteId: {}, URL 개수: {}, Error: {}",
                    event.websiteId().getId(), event.urlCount(), e.getMessage(), e);
            throw e;  // acknowledge 없이 throw → Kafka가 메시지 재시도
        }
    }

    /**
     * 인프라 레이어 작업: Redis 캐시 + DB 배치 조회로 중복 체크
     * (도메인 로직은 이미 Website.filterValidUrls()에서 처리됨)
     */
    private List<String> filterNewUrlsFromCache(List<String> validUrls, WebsiteId websiteId) {

        // 1단계: Redis 캐시에서 빠른 필터링
        Set<String> cacheFilteredUrls = crawlCachePort.filterNewUrls(websiteId, validUrls);

        if (cacheFilteredUrls.isEmpty()) {
            log.debug("모든 URL이 캐시에서 필터링됨 - 전체: {}", validUrls.size());
            return List.of();
        }

        // 2단계: 캐시에 없는 URL들만 DB에서 배치 조회
        List<String> urlsToCheck = List.copyOf(cacheFilteredUrls);
        Set<String> existingInDb = saveCrawledUrlPort.findExistingUrls(websiteId, urlsToCheck);

        // 3단계: DB에서도 새로운 URL들만 최종 필터링
        List<String> finalNewUrls = urlsToCheck.stream()
            .filter(url -> !existingInDb.contains(url))
            .collect(Collectors.toList());

        // 4단계: 새로운 URL들을 캐시에 저장 (다음 요청 시 빠른 필터링)
        if (!finalNewUrls.isEmpty()) {
            crawlCachePort.cacheUrls(websiteId, finalNewUrls);
        }

        log.debug("중복 체크 완료 - 전체: {}, 캐시 후: {}, DB 후: {}, 최종: {}",
                validUrls.size(), cacheFilteredUrls.size(), urlsToCheck.size() - existingInDb.size(), finalNewUrls.size());

        return finalNewUrls;
    }

    /**
     * 새로운 크롤링 이벤트들을 배치로 발행
     */
    private void publishCrawlingEvents(DiscoveredUrlsEvent event, List<String> newUrls) {
        int publishedCount = 0;

        for (String url : newUrls) {
            try {
                UrlCrawlEvent crawlEvent = UrlCrawlEvent.createChildCrawl(
                    event.websiteId(),
                    url,
                    event.parentUrl(),
                    event.depth() + 1
                );

                eventDispatcher.dispatch(crawlEvent);
                publishedCount++;

            } catch (Exception e) {
                log.warn("개별 크롤링 이벤트 발행 실패 - URL: {}, Error: {}", url, e.getMessage());
            }
        }

        log.info("크롤링 이벤트 발행 완료 - 성공: {}/{}", publishedCount, newUrls.size());
    }

    /**
     * URL 유효성 검사
     */
    private boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        // 기본적인 URL 형식 검사
        return url.startsWith("http://") || url.startsWith("https://");
    }
}