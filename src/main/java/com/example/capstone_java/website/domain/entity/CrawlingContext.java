package com.example.capstone_java.website.domain.entity;

import com.example.capstone_java.website.domain.event.DiscoveredUrlsEvent;
import com.example.capstone_java.website.domain.event.UrlCrawlEvent;

import java.util.List;
import java.util.Optional;

/**
 * 크롤링 상태와 행동을 캡슐화한 컨텍스트 객체
 * Tell, Don't Ask 원칙에 따라 스스로 상태를 관리하고 다음 단계를 결정
 */
public final class CrawlingContext {

    private final Website website;
    private final UrlCrawlEvent event;
    private final List<String> discoveredUrls;
    private final CrawlStatus status;
    private final String skipReason;

    private CrawlingContext(Website website, UrlCrawlEvent event, List<String> discoveredUrls,
                           CrawlStatus status, String skipReason) {
        this.website = website;
        this.event = event;
        this.discoveredUrls = List.copyOf(discoveredUrls);
        this.status = status;
        this.skipReason = skipReason;
    }

    /**
     * 크롤링 컨텍스트 시작
     */
    public static CrawlingContext start(Website website, UrlCrawlEvent event) {
        return new CrawlingContext(website, event, List.of(), CrawlStatus.PENDING, null);
    }

    /**
     * 스스로 깊이를 검증하고 다음 상태로 전환
     */
    public CrawlingContext validateDepth() {
        if (!website.canCrawlAtDepth(event.depth())) {
            return skip("최대 크롤링 깊이 도달: " + event.depth());
        }
        return proceedToReady();
    }

    /**
     * 발견된 URL들을 받아서 스스로 필터링하고 상태 변경
     */
    public CrawlingContext withDiscoveredUrls(List<String> rawUrls) {
        if (status == CrawlStatus.SKIPPED) {
            return this; // 이미 스킵된 상태면 그대로 유지
        }

        // 도메인이 직접 필터링
        List<String> validUrls = website.filterValidUrls(rawUrls);
        List<String> finalUrls = website.excludeParentUrl(validUrls, event.url());

        return new CrawlingContext(website, event, finalUrls, CrawlStatus.DISCOVERED, null);
    }

    /**
     * 자신의 상태를 보고 이벤트 생성 여부를 결정
     */
    public Optional<DiscoveredUrlsEvent> toEventIfReady() {
        if (status == CrawlStatus.DISCOVERED && !discoveredUrls.isEmpty()) {
            return Optional.of(DiscoveredUrlsEvent.create(
                event.websiteId(),
                event.url(),
                discoveredUrls,
                event.depth()
            ));
        }
        return Optional.empty();
    }

    /**
     * 크롤링된 URL 도메인 객체 생성
     */
    public CrawledUrl toCrawledUrl() {
        // 루트 URL 판단: parentUrl이 null이고 depth가 0인 경우
        boolean isRootUrl = event.parentUrl() == null && event.depth() == 0;

        CrawledUrl crawledUrl = isRootUrl
            ? CrawledUrl.rootUrl(event.websiteId(), event.url())
            : CrawledUrl.discovered(event.websiteId(), event.url(), event.parentUrl(), event.depth());

        return crawledUrl.markCrawled();
    }

    /**
     * 크롤링을 계속 진행해야 하는지 판단
     */
    public boolean shouldProceed() {
        return status == CrawlStatus.READY || status == CrawlStatus.PENDING;
    }

    /**
     * 크롤링 결과 요약 정보
     */
    public String getSummary() {
        if (status == CrawlStatus.SKIPPED) {
            return String.format("크롤링 스킵: %s (URL: %s)", skipReason, event.url());
        }
        return String.format("크롤링 완료: URL=%s, 발견된 URL 수=%d", event.url(), discoveredUrls.size());
    }

    // 내부 상태 전환 메서드들
    private CrawlingContext skip(String reason) {
        return new CrawlingContext(website, event, List.of(), CrawlStatus.SKIPPED, reason);
    }

    private CrawlingContext proceedToReady() {
        return new CrawlingContext(website, event, discoveredUrls, CrawlStatus.READY, null);
    }

    // 크롤링 상태 열거형
    private enum CrawlStatus {
        PENDING,    // 시작됨
        READY,      // 검증 완료, 크롤링 준비됨
        DISCOVERED, // URL 발견 및 필터링 완료
        SKIPPED     // 크롤링 스킵됨
    }
}