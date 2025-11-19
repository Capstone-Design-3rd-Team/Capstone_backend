package com.example.capstone_java.website.domain.event;

import com.example.capstone_java.website.domain.vo.WebsiteId;

import java.time.LocalDateTime;

/**
 * URL 접근성 분석 요청 이벤트
 * AI 서버로 분석을 요청하기 위해 발행되는 이벤트
 */
public record UrlAnalysisRequestEvent(
        WebsiteId websiteId,
        String url,
        String callbackUrl,
        int depth,
        LocalDateTime eventOccurredAt
) implements DomainEvent {

    @Override
    public LocalDateTime occurredAt() {
        return eventOccurredAt;
    }

    /**
     * 새로운 URL 분석 요청 이벤트 생성
     */
    public static UrlAnalysisRequestEvent create(WebsiteId websiteId, String url, String callbackUrl, int depth) {
        return new UrlAnalysisRequestEvent(websiteId, url, callbackUrl, depth, LocalDateTime.now());
    }
}
