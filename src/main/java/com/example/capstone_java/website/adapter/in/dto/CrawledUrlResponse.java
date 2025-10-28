package com.example.capstone_java.website.adapter.in.dto;

public record CrawledUrlResponse(
        String url,
        String parentUrl,
        int depth,
        String status,
        java.time.LocalDateTime discoveredAt,
        java.time.LocalDateTime crawledAt
) {
}
