package com.example.capstone_java.website.domain.event;

import com.example.capstone_java.website.domain.vo.WebsiteId;
import java.time.LocalDateTime;

public record ExtractionStartedEvent(
        WebsiteId websiteId,
        String mainUrl,
        LocalDateTime eventOccurredAt
) {

    public static ExtractionStartedEvent of(WebsiteId websiteId, String mainUrl) {
        return new ExtractionStartedEvent(websiteId, mainUrl, LocalDateTime.now());
    }
    /**
     * Kafka 토픽 키 생성 (파티션 분산용)
     */
    public String getPartitionKey() {
        return websiteId != null ? websiteId.getId().toString() : "default";
    }

}
