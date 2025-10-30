package com.example.capstone_java.website.adapter.out.persistence.entity;

import com.example.capstone_java.website.domain.entity.ExtractionStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "website")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WebsiteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID websiteId;

    @Column(nullable = false, length = 2048)
    private String mainUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExtractionStatus extractionStatus;

    // CrawlConfiguration 관련 필드들
    @Column(name = "max_depth", nullable = false)
    private Integer maxDepth;

    @Column(name = "max_total_urls", nullable = false)
    private Integer maxTotalUrls;

    @Column(name = "max_urls_per_page", nullable = false)
    private Integer maxUrlsPerPage;

    @Column(name = "max_duration_minutes", nullable = false)
    private Long maxDurationMinutes;

    @Column(name = "allowed_paths", length = 1000)
    private String allowedPaths; // JSON 문자열로 저장

    @Column(name = "excluded_paths", length = 1000)
    private String excludedPaths; // JSON 문자열로 저장

    @Column(nullable = false)
    private LocalDateTime createdAt;
}