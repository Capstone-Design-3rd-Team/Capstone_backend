package com.example.capstone_java.website.adapter.in.dto;

import java.util.UUID;

public record CrawlStartResponse(
        UUID websiteId,
        String mainUrl,
        String message
) {
}
