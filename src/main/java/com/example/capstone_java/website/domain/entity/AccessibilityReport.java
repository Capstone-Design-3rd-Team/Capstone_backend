package com.example.capstone_java.website.domain.entity;

import com.example.capstone_java.website.adapter.in.dto.AiAnalysisResponse;
import com.example.capstone_java.website.domain.vo.WebsiteId;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 접근성 분석 보고서 도메인 엔티티
 *
 * AI 서버로부터 받은 접근성 분석 결과를 도메인 객체로 표현
 */
@Getter
@AllArgsConstructor
public class AccessibilityReport {
    private Long id;
    private WebsiteId websiteId;
    private String url;
    private Map<String, Object> analysisResult;  // AI가 보낸 전체 JSON 결과
    private LocalDateTime analyzedAt;
    private String taskId;  // AI 서버의 작업 ID

    // 주요 분석 결과 (JSON에서 추출된 값들)
    private Integer totalButtons;
    private Integer accessibleButtons;
    private Double accessibilityScore;
    private String screenshotPath;

    /**
     * AI 분석 결과로부터 새로운 보고서 생성 (ID 없음)
     *
     * @param websiteId 웹사이트 ID
     * @param url 분석된 URL
     * @param analysisResult 전체 JSON 결과 (DB 저장용)
     * @param aiResponse 파싱된 DTO (값 추출용)
     * @param taskId AI 작업 ID
     */
    public static AccessibilityReport create(
            WebsiteId websiteId,
            String url,
            Map<String, Object> analysisResult,
            AiAnalysisResponse aiResponse,
            String taskId) {

        // DTO에서 주요 값 추출 (실제 AI 응답 구조에 맞게)
        Integer totalButtons = aiResponse.getCrawledButtonCount();  // crawled_button_count를 total로 사용
        Integer accessibleButtons = aiResponse.getDetectedButtonCount();  // detected_button_count를 accessible로 사용
        Double accessibilityScore = aiResponse.getAccessibilityScore();  // results.summary.final_score
        String screenshotPath = aiResponse.getS3ScreenshotUrl();  // results.analysis_info.s3_url

        return new AccessibilityReport(
            null,  // ID는 JPA가 자동 생성
            websiteId,
            url,
            analysisResult,  // 전체 JSON 저장 (향후 확장성)
            LocalDateTime.now(),
            taskId,
            totalButtons,
            accessibleButtons,
            accessibilityScore,
            screenshotPath
        );
    }

    /**
     * DB에서 조회한 보고서 재구성 (ID 있음)
     */
    public static AccessibilityReport withId(Long id, WebsiteId websiteId, String url, Map<String, Object> analysisResult,
                                            LocalDateTime analyzedAt, String taskId, Integer totalButtons,
                                            Integer accessibleButtons, Double accessibilityScore, String screenshotPath) {
        return new AccessibilityReport(
            id,
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

    // === 도메인 비즈니스 로직 ===

    /**
     * 접근성이 양호한지 판단 (점수 70점 이상)
     */
    public boolean hasGoodAccessibility() {
        return this.accessibilityScore != null && this.accessibilityScore >= 70.0;
    }

    /**
     * 접근성이 불량한지 판단 (점수 50점 미만)
     */
    public boolean hasPoorAccessibility() {
        return this.accessibilityScore != null && this.accessibilityScore < 50.0;
    }

    /**
     * 버튼 분석이 완료되었는지 확인
     */
    public boolean hasButtonAnalysis() {
        return this.totalButtons != null && this.totalButtons > 0;
    }

    /**
     * 접근 가능한 버튼 비율 계산
     */
    public double getAccessibleButtonRatio() {
        if (this.totalButtons == null || this.totalButtons == 0) {
            return 0.0;
        }
        if (this.accessibleButtons == null) {
            return 0.0;
        }
        return (double) this.accessibleButtons / this.totalButtons * 100.0;
    }
}
