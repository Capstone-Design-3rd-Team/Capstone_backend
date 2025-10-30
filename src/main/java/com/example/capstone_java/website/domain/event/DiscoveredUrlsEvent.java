package com.example.capstone_java.website.domain.event;

import com.example.capstone_java.website.domain.vo.WebsiteId;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 크롤링 결과로 발견된 URL들을 배치로 처리하기 위한 도메인 이벤트
 * JobUpdatingConsumer에서 중복 체크와 새로운 크롤링 작업 생성을 배치로 처리
 */
public record DiscoveredUrlsEvent(
        WebsiteId websiteId,
        String parentUrl,
        List<String> discoveredUrls,
        int depth,
        LocalDateTime eventOccurredAt
) implements DomainEvent {

    @Override
    public LocalDateTime occurredAt() {
        return eventOccurredAt;
    }

    public static DiscoveredUrlsEvent create(
            WebsiteId websiteId,
            String parentUrl,
            List<String> discoveredUrls,
            int depth
    ) {
        return new DiscoveredUrlsEvent(
                websiteId,
                parentUrl,
                List.copyOf(discoveredUrls), // 불변 리스트로 복사
                depth,
                LocalDateTime.now()
        );
    }

    /**
     * 파티션 키: parentUrl을 사용하여 분산
     * - 같은 부모 URL에서 발견된 자식 URL들은 순서 보장
     * - 다른 부모 URL들은 병렬 처리
     */
    public String getPartitionKey() {
        return parentUrl != null ? parentUrl : "root";
    }

    public int urlCount() {
        return discoveredUrls.size();
    }
}