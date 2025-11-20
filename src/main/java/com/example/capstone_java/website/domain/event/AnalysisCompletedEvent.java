package com.example.capstone_java.website.domain.event;

import com.example.capstone_java.website.domain.vo.WebsiteId;
import java.time.LocalDateTime;

/**
 * AI 분석 결과 저장 완료 이벤트
 *
 * 발행 시점: AccessibilityReport가 DB에 저장되고 트랜잭션이 커밋된 직후
 * 용도: SSE를 통해 프론트엔드에 진행 상황 알림 (트랜잭션 커밋 후라서 count가 정확함)
 */
public record AnalysisCompletedEvent(
        WebsiteId websiteId,
        LocalDateTime eventOccurredAt
) implements DomainEvent {

    @Override
    public LocalDateTime occurredAt() {
        return eventOccurredAt;
    }

    public static AnalysisCompletedEvent of(WebsiteId websiteId) {
        return new AnalysisCompletedEvent(websiteId, LocalDateTime.now());
    }
}
