package com.example.capstone_java.website.domain.entity;

import com.example.capstone_java.website.domain.vo.WebsiteId;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public final class Website {
    private final WebsiteId websiteId;
    private final String mainUrl;
    private final ExtractionStatus extractionStatus;
    private final LocalDateTime creationDateTime;

    private Website(final WebsiteId websiteId, final String mainUrl, ExtractionStatus extractionStatus,  final LocalDateTime creationDateTime) {
        this.websiteId = websiteId;
        this.mainUrl = mainUrl;
        this.extractionStatus = extractionStatus;
        this.creationDateTime = creationDateTime;
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
        return new Website(this.websiteId, this.mainUrl, ExtractionStatus.PROGRESS, this.creationDateTime);
    }

    public Website markCompleted() {
        return new Website(this.websiteId, this.mainUrl, ExtractionStatus.COMPLETE, this.creationDateTime);
    }

    public Website markFailed() {
        return new Website(this.websiteId, this.mainUrl, ExtractionStatus.FAILED, this.creationDateTime);
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
