package com.example.capstone_java.website.adapter.out.persistence.entity;

import com.example.capstone_java.website.domain.entity.CrawlStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "crawled_url", indexes = {
    @Index(name = "idx_website_id", columnList = "website_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_website_status", columnList = "website_id, status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrawledUrlEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "website_id", nullable = false)
    private UUID websiteId;

    @Column(name = "url", nullable = false, length = 2048)
    private String url;

    @Column(name = "parent_url", length = 2048)
    private String parentUrl;

    @Column(name = "depth", nullable = false)
    private Integer depth;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CrawlStatus status;

    @Column(name = "discovered_at", nullable = false)
    private LocalDateTime discoveredAt;

    @Column(name = "crawled_at")
    private LocalDateTime crawledAt;

    // 생성자
    public CrawledUrlEntity(UUID websiteId, String url, String parentUrl, Integer depth,
                           CrawlStatus status, LocalDateTime discoveredAt, LocalDateTime crawledAt) {
        this.websiteId = websiteId;
        this.url = url;
        this.parentUrl = parentUrl;
        this.depth = depth;
        this.status = status;
        this.discoveredAt = discoveredAt;
        this.crawledAt = crawledAt;
    }

    // 정적 팩토리 메서드
    public static CrawledUrlEntity create(UUID websiteId, String url, String parentUrl, Integer depth,
                                         CrawlStatus status, LocalDateTime discoveredAt, LocalDateTime crawledAt) {
        return new CrawledUrlEntity(websiteId, url, parentUrl, depth, status, discoveredAt, crawledAt);
    }
}