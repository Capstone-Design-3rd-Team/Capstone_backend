package com.example.capstone_java.website.adapter.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 최종 보고서 DTO (전체 웹사이트 분석 결과 종합)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "최종 분석 결과 보고서 (event: complete)")
public class FinalReportDto {
    @Schema(description = "분석 대상 웹사이트 URL", example = "https://www.example.com")
    private String websiteUrl;

    @Schema(description = "클라이언트 ID", example = "client_12345")
    private String clientId;

    @Schema(description = "총 분석된 페이지 수", example = "100")
    private int totalAnalyzedUrls;

    @Schema(description = "전체 평균 접근성 점수", example = "85.5")
    private Double averageScore;

    @Schema(description = "전체 접근성 등급", example = "Good")
    private String overallLevel;

    @Schema(description = "심각도 수준", example = "Low")
    private String severityLevel;

    @Schema(description = "상세 통계 정보")
    private StatisticsDto statistics;

    @Schema(description = "페이지별 상세 보고서 목록")
    private List<UrlDetailReportDto> urlReports;

    @Schema(description = "개선 권장 사항 목록")
    private List<String> recommendations;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "세부 통계 데이터")
    public static class StatisticsDto {
        // === 버튼 관련 점수 ===
        @Schema(description = "평균 버튼 탐지 점수 (AI 인식률)", example = "92.5")
        private Double averageButtonDetectionScore;

        @Schema(description = "평균 버튼 크기 점수 (터치 타겟 적절성)", example = "85.0")
        private Double averageButtonSizeScore;

        @Schema(description = "평균 버튼 명도 대비 점수 (가시성)", example = "90.5")
        private Double averageButtonContrastScore;

        @Schema(description = "평균 버튼 피드백 점수 (호버/포커스 효과 유무)", example = "70.0")
        private Double averageButtonFeedbackScore;

        // === 텍스트 관련 점수 ===
        @Schema(description = "평균 폰트 크기 점수 (가독성)", example = "88.5")
        private Double averageFontSizeScore;

        @Schema(description = "평균 텍스트 명도 대비 점수 (배경과 글자색)", example = "95.0")
        private Double averageContrastScore;

        @Schema(description = "평균 한국어 비율 점수", example = "99.9")
        private Double averageKoreanRatioScore;

        // === 버튼 개수 통계 ===
        @Schema(description = "AI가 최종적으로 인식한 총 버튼 수", example = "150")
        private Integer totalButtonsDetected;

        @Schema(description = "HTML 태그상 발견된 총 버튼 수 (크롤링 기준)", example = "160")
        private Integer totalButtonsCrawled;

        // === 점수 분포 (도넛 차트 등에 사용) ===
        @Schema(description = "최우수 등급(80점 이상) 페이지 수", example = "15")
        private int excellentCount;

        @Schema(description = "우수 등급(60~79점) 페이지 수", example = "8")
        private int goodCount;

        @Schema(description = "보통 등급(40~59점) 페이지 수", example = "5")
        private int fairCount;

        @Schema(description = "미흡 등급(40점 미만) 페이지 수", example = "2")
        private int poorCount;
    }
}
