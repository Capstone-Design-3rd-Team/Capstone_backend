package com.example.capstone_java.website.adapter.in.dto;

import java.util.UUID;

public record WebsiteStatusResponse(
        UUID websiteId,
        String mainUrl,
        String status,
        int maxDepth,
        int maxTotalUrls,
        java.time.LocalDateTime createdAt
) {
}
