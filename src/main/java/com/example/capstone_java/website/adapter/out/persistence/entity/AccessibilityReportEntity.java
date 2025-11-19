package com.example.capstone_java.website.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 접근성 분석 보고서 JPA 엔티티
 *
 * AI 서버로부터 받은 분석 결과를 DB에 저장하기 위한 영속성 엔티티
 */
@Entity
@Table(name = "accessibility_report", indexes = {
    @Index(name = "idx_website_id", columnList = "website_id"),
    @Index(name = "idx_task_id", columnList = "task_id"),
    @Index(name = "idx_website_analyzed", columnList = "website_id, analyzed_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccessibilityReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "website_id", nullable = false)
    private UUID websiteId;

    @Column(name = "url", nullable = false, length = 2048)
    private String url;

    /**
     * AI가 보낸 전체 JSON 결과를 JSON 타입으로 저장
     * MySQL 5.7.8+ 또는 PostgreSQL에서 지원
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "analysis_result", columnDefinition = "json")
    private Map<String, Object> analysisResult;

    @Column(name = "analyzed_at", nullable = false)
    private LocalDateTime analyzedAt;

    @Column(name = "task_id", length = 100)
    private String taskId;

    // 주요 분석 결과 (빠른 조회를 위해 별도 컬럼으로 저장)
    @Column(name = "total_buttons")
    private Integer totalButtons;

    @Column(name = "accessible_buttons")
    private Integer accessibleButtons;

    @Column(name = "accessibility_score")
    private Double accessibilityScore;

    @Column(name = "screenshot_path", length = 500)
    private String screenshotPath;

    // 생성자
    private AccessibilityReportEntity(UUID websiteId, String url, Map<String, Object> analysisResult,
                                     LocalDateTime analyzedAt, String taskId, Integer totalButtons,
                                     Integer accessibleButtons, Double accessibilityScore, String screenshotPath) {
        this.websiteId = websiteId;
        this.url = url;
        this.analysisResult = analysisResult;
        this.analyzedAt = analyzedAt;
        this.taskId = taskId;
        this.totalButtons = totalButtons;
        this.accessibleButtons = accessibleButtons;
        this.accessibilityScore = accessibilityScore;
        this.screenshotPath = screenshotPath;
    }

    // 정적 팩토리 메서드
    public static AccessibilityReportEntity create(UUID websiteId, String url, Map<String, Object> analysisResult,
                                                   LocalDateTime analyzedAt, String taskId, Integer totalButtons,
                                                   Integer accessibleButtons, Double accessibilityScore,
                                                   String screenshotPath) {
        return new AccessibilityReportEntity(
            websiteId,
            url,
            analysisResult,
            analyzedAt,
            taskId,
            totalButtons,
            accessibleButtons,
            accessibilityScore,
            screenshotPath
        );
    }
}
