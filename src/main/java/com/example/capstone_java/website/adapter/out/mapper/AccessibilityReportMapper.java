package com.example.capstone_java.website.adapter.out.mapper;

import com.example.capstone_java.website.adapter.out.persistence.entity.AccessibilityReportEntity;
import com.example.capstone_java.website.domain.entity.AccessibilityReport;
import com.example.capstone_java.website.domain.vo.WebsiteId;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.util.UUID;

/**
 * AccessibilityReport 도메인 객체와 AccessibilityReportEntity 간 매핑
 */
@Mapper(componentModel = "spring")
public interface AccessibilityReportMapper {

    @Named("websiteIdToUuid")
    default UUID websiteIdToUuid(WebsiteId websiteId) {
        return websiteId != null ? websiteId.getId() : null;
    }

    @Named("uuidToWebsiteId")
    default WebsiteId uuidToWebsiteId(UUID uuid) {
        return uuid != null ? WebsiteId.of(uuid) : null;
    }

    /**
     * JPA 엔티티를 도메인 객체로 변환 (ID 포함)
     */
    default AccessibilityReport toDomainWithId(AccessibilityReportEntity entity) {
        if (entity == null) {
            return null;
        }

        return AccessibilityReport.withId(
            entity.getId(),
            WebsiteId.of(entity.getWebsiteId()),
            entity.getUrl(),
            entity.getAnalysisResult(),
            entity.getAnalyzedAt(),
            entity.getTaskId(),
            entity.getTotalButtons(),
            entity.getAccessibleButtons(),
            entity.getAccessibilityScore(),
            entity.getScreenshotPath()
        );
    }

    /**
     * 도메인 객체를 JPA 엔티티로 변환
     */
    default AccessibilityReportEntity toEntityFromDomain(AccessibilityReport report) {
        if (report == null) {
            return null;
        }

        return AccessibilityReportEntity.create(
            report.getWebsiteId().getId(),
            report.getUrl(),
            report.getAnalysisResult(),
            report.getAnalyzedAt(),
            report.getTaskId(),
            report.getTotalButtons(),
            report.getAccessibleButtons(),
            report.getAccessibilityScore(),
            report.getScreenshotPath()
        );
    }
}
