package com.example.capstone_java.website.adapter.in.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * AI 서버 분석 결과 응답 DTO
 *
 * 실제 AI 응답 구조:
 * {
 *   "task_id": "7cfe542f3b8a2011",
 *   "website_id": "123",
 *   "results": {
 *     "analysis_info": { "url": "...", "s3_url": "...", ... },
 *     "button_analysis": { "crawled_button_count": 17, "detected_button_count": 7, ... },
 *     "summary": { "final_score": 53.57, ... },
 *     "detailed_scores": { ... },
 *     "issues": [ ... ],
 *     "recommendations": [ ... ],
 *     "scroll_info": { ... }
 *   }
 * }
 */
@Getter
@Setter // [추가] JSON 데이터를 담으려면 Setter가 필요합니다.
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiAnalysisResponse {
    @JsonProperty("task_id")
    private String taskId;

    // [삭제] website_id는 여기가 아니라 AnalysisInfo 안에 있습니다.
    @JsonProperty("website_id")
    private String websiteId;

    @JsonProperty("results") // [추가] 명시적으로 적어주는 게 안전함
    private Results results;

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Results {

        @JsonProperty("analysis_info")
        private AnalysisInfo analysisInfo;

        @JsonProperty("button_analysis")
        private ButtonAnalysis buttonAnalysis;

        @JsonProperty("summary")
        private Summary summary;

        @JsonProperty("detailed_scores")
        private Map<String, Object> detailedScores;

        @JsonProperty("issues")
        private List<Map<String, Object>> issues;

        @JsonProperty("recommendations")
        private List<Map<String, Object>> recommendations;

        @JsonProperty("scroll_info")
        private Map<String, Object> scrollInfo;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AnalysisInfo {

        @JsonProperty("url") // [중요] 이게 없어서 url이 null이 되었습니다.
        private String url;

        @JsonProperty("s3_url")
        private String s3Url;

        @JsonProperty("screenshot_path")
        private String screenshotPath;

        @JsonProperty("analysis_date")
        private String analysisDate;

        @JsonProperty("website_id") // [이동] JSON 구조에 맞춰 여기로 이동
        private String websiteId;

        @JsonProperty("task_id") // [참고] JSON에 여기도 task_id가 있어서 추가해둠
        private String taskId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ButtonAnalysis {

        @JsonProperty("crawled_button_count")
        private Integer crawledButtonCount;

        @JsonProperty("detected_button_count")
        private Integer detectedButtonCount;

        @JsonProperty("button_count_difference")
        private Integer buttonCountDifference;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Summary {

        @JsonProperty("final_score")
        private Double finalScore;

        @JsonProperty("accessibility_level")
        private String accessibilityLevel;

        @JsonProperty("color")
        private String color;

        @JsonProperty("severity_level")
        private String severityLevel;
    }

    // === 편의 메서드 ===

    /**
     * 분석된 URL 반환
     */
    public String getUrl() {
        return results != null && results.analysisInfo != null
            ? results.analysisInfo.url
            : null;
    }

    /**
     * 접근성 점수 반환
     */
    public Double getAccessibilityScore() {
        return results != null && results.summary != null
            ? results.summary.finalScore
            : null;
    }

    /**
     * S3 스크린샷 URL 반환
     */
    public String getS3ScreenshotUrl() {
        return results != null && results.analysisInfo != null
            ? results.analysisInfo.s3Url
            : null;
    }

    /**
     * 크롤링된 버튼 수 반환
     */
    public Integer getCrawledButtonCount() {
        return results != null && results.buttonAnalysis != null
            ? results.buttonAnalysis.crawledButtonCount
            : null;
    }

    /**
     * 탐지된 버튼 수 반환
     */
    public Integer getDetectedButtonCount() {
        return results != null && results.buttonAnalysis != null
            ? results.buttonAnalysis.detectedButtonCount
            : null;
    }

    public String getWebsiteId() {
        return results != null && results.getAnalysisInfo() != null
                ? results.getAnalysisInfo().getWebsiteId()
                : null;
    }
}
