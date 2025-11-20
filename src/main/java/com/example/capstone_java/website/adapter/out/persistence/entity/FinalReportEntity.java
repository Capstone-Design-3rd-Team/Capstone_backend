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
 * 최종 보고서 엔티티
 *
 * 책임: 완성된 최종 보고서를 DB에 영구 저장
 * - SSE로 큰 JSON을 보내는 대신 DB에 저장
 * - 프론트엔드가 GET API로 안정적으로 조회
 * - 재다운로드 가능
 * - 히스토리 관리 가능
 */
@Entity
@Table(name = "final_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FinalReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Website ID (FK는 아니지만 연관 관계)
     */
    @Column(name = "website_id", nullable = false, unique = true)
    private UUID websiteId;

    /**
     * 최종 보고서 전체 JSON (FinalReportDto를 JSON으로 직렬화)
     *
     * MySQL 8.0+ JSON 타입 사용
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "report_json", columnDefinition = "JSON", nullable = false)
    private Map<String, Object> reportJson;

    /**
     * 평균 점수 (빠른 조회용)
     */
    @Column(name = "average_score")
    private Double averageScore;

    /**
     * 분석된 URL 개수 (빠른 조회용)
     */
    @Column(name = "analyzed_url_count")
    private Integer analyzedUrlCount;

    /**
     * 생성 일시
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 정적 팩토리 메서드
     */
    public static FinalReportEntity create(UUID websiteId, Map<String, Object> reportJson, Double averageScore, Integer analyzedUrlCount) {
        FinalReportEntity entity = new FinalReportEntity();
        entity.websiteId = websiteId;
        entity.reportJson = reportJson;
        entity.averageScore = averageScore;
        entity.analyzedUrlCount = analyzedUrlCount;
        entity.createdAt = LocalDateTime.now();
        return entity;
    }
}
