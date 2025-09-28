package com.example.capstone_java.website.domain.entity;

import com.example.capstone_java.website.domain.vo.WebsiteId;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public final class Website {
    private final WebsiteId websiteId;
    private final String mainUrl;
    private final ExtractionStatus extractionStatus;
    private final LocalDateTime createdAt;

    // MapStruct용 public 생성자 (하나만 유지)
    public Website(WebsiteId websiteId, String mainUrl, ExtractionStatus extractionStatus, LocalDateTime createdAt) {
        this.websiteId = websiteId;
        this.mainUrl = mainUrl;
        this.extractionStatus = extractionStatus;
        this.createdAt = createdAt;
    }

    public static Website create(final String mainUrl) {
        return new Website(null, mainUrl, ExtractionStatus.PENDING, LocalDateTime.now());
    }

    public static Website withId(final WebsiteId websiteId,
                                 final String mainUrl,
                                 final ExtractionStatus extractionStatus,
                                 final LocalDateTime creationDateTime)
    {
        return new Website(websiteId, mainUrl, extractionStatus, creationDateTime);
    }

    public Website startExtraction() {
        if (this.extractionStatus != ExtractionStatus.PENDING) {
            throw new IllegalStateException("추출이 이미 시작되었습니다.");
        }
        return new Website(this.websiteId, this.mainUrl, ExtractionStatus.PROGRESS, this.createdAt);
    }

    public Website markCompleted() {
        return new Website(this.websiteId, this.mainUrl, ExtractionStatus.COMPLETE, this.createdAt);
    }

    public Website markFailed() {
        return new Website(this.websiteId, this.mainUrl, ExtractionStatus.FAILED, this.createdAt);
    }

    public boolean isCompleted() {
        return extractionStatus == ExtractionStatus.COMPLETE;
    }

    public boolean isFailed() {
        return extractionStatus == ExtractionStatus.FAILED;
    }

    public boolean isInProgress() {
        return extractionStatus == ExtractionStatus.PROGRESS;
    }

    public boolean isPending() {
        return extractionStatus == ExtractionStatus.PENDING;
    }
}
