package com.example.capstone_java.website.domain.event;

import com.example.capstone_java.website.domain.vo.WebsiteId;
import java.time.LocalDateTime;

public record ExtractionStartedEvent(
        WebsiteId websiteId,
        String mainUrl,
        LocalDateTime eventOccurredAt
) implements DomainEvent {

    @Override
    public LocalDateTime occurredAt() {
        return eventOccurredAt;
    }

    public static ExtractionStartedEvent of(WebsiteId websiteId, String mainUrl) {
        return new ExtractionStartedEvent(websiteId, mainUrl, LocalDateTime.now());
    }
}
