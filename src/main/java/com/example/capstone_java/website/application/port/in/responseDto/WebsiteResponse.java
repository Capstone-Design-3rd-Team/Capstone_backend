package com.example.capstone_java.website.application.port.in.responseDto;

import com.example.capstone_java.website.domain.entity.ExtractionStatus;
import com.example.capstone_java.website.domain.vo.WebsiteId;

import java.time.LocalDateTime;

public record WebsiteResponse(
        WebsiteId websiteId,
        String mainUrl,
        ExtractionStatus extractionStatus,
        LocalDateTime createdAt
) {
}
