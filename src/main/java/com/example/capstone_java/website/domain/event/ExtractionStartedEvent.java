package com.example.capstone_java.website.domain.event;

import com.example.capstone_java.website.domain.vo.WebsiteId;

public record ExtractionStartedEvent(
        WebsiteId websiteId
) {
}
