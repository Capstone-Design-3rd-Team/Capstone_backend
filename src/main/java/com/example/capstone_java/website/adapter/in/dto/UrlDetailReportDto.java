package com.example.capstone_java.website.adapter.in.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * URL별 상세 분석 보고서 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UrlDetailReportDto {

    // === 기본 정보 ===
    private String url;                     // 분석 대상 URL
    private String analysisDate;            // 분석 수행 일시
    private String screenshotPath;          // 스크린샷 저장 경로
    private String s3Url;                   // S3 업로드 URL
    private String taskId;                  // AI Task ID
    private String websiteId;               // 웹사이트 ID

    // === 스크롤 정보 ===
    private Boolean verticalScroll;         // 수직 스크롤 여부
    private Boolean horizontalScroll;       // 수평 스크롤 여부

    // === 버튼 분석 ===
    private Integer crawledButtonCount;     // 크롤링된 버튼 수
    private Integer detectedButtonCount;    // AI 탐지 버튼 수
    private Integer buttonCountDifference;  // 버튼 개수 차이

    // === 상세 점수 ===
    private DetailScoreDto buttonDetection;       // 버튼 탐지
    private DetailScoreDto buttonVisualFeedback;  // 버튼 시각적 피드백
    private DetailScoreDto buttonSize;            // 버튼 크기
    private DetailScoreDto buttonContrast;        // 버튼 명암 대비
    private DetailScoreDto fontSize;              // 폰트 크기
    private DetailScoreDto overallContrast;       // 전체 대비
    private DetailScoreDto koreanRatio;           // 한국어 텍스트 비율

    // === 종합 점수 ===
    private Double finalScore;              // 최종 점수
    private String accessibilityLevel;      // 접근성 수준
    private String severityLevel;           // 심각도 수준

    // === 텍스트 보고서 ===
    private String textReport;              // 생성된 텍스트 보고서

    /**
     * 상세 점수 DTO (각 항목별)
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DetailScoreDto {
        private Double score;               // 점수
        private String level;               // High, Medium, Low
        private Double weight;              // 가중치
        private String recommendation;      // 권장사항
    }
}
