package com.example.capstone_java.website.application.service;

import com.example.capstone_java.website.adapter.in.dto.FinalReportDto;
import com.example.capstone_java.website.adapter.in.dto.UrlDetailReportDto;
import com.example.capstone_java.website.domain.entity.AccessibilityReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 접근성 분석 보고서 생성 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGenerationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * AccessibilityReport로부터 URL별 상세 보고서 생성
     */
    public UrlDetailReportDto generateUrlDetailReport(AccessibilityReport report) {
        log.debug("🔍 개별 URL 보고서 생성: url={}, score={}", report.getUrl(), report.getAccessibilityScore());

        // AI 응답 구조: { "task_id": ..., "website_id": ..., "results": { ... } }
        Map<String, Object> analysisResult = report.getAnalysisResult();
        log.debug("🔍 analysisResult 최상위 키: {}", analysisResult.keySet());

        // "results" 키 안에 실제 분석 결과가 있음
        Map<String, Object> results = getMap(analysisResult, "results");
        log.debug("🔍 results 키: {}", results.keySet());

        // 기본 정보 추출
        Map<String, Object> analysisInfo = getMap(results, "analysis_info");
        Map<String, Object> scrollInfo = getMap(results, "scroll_info");
        Map<String, Object> buttonAnalysis = getMap(results, "button_analysis");
        Map<String, Object> detailedScores = getMap(results, "detailed_scores");
        Map<String, Object> summary = getMap(results, "summary");

        log.debug("🔍 추출된 값 - url: {}, analysisDate: {}, s3Url: {}",
            getString(analysisInfo, "url"),
            getString(analysisInfo, "analysis_date"),
            getString(analysisInfo, "s3_url"));
        log.debug("🔍 추출된 값 - crawledButtonCount: {}, detectedButtonCount: {}",
            getInteger(buttonAnalysis, "crawled_button_count"),
            getInteger(buttonAnalysis, "detected_button_count"));

        // 상세 점수 추출
        UrlDetailReportDto.DetailScoreDto buttonDetection = extractDetailScore(detailedScores, "button_detection");
        UrlDetailReportDto.DetailScoreDto buttonVisualFeedback = extractDetailScore(detailedScores, "button_visual_feedback");
        UrlDetailReportDto.DetailScoreDto buttonSize = extractDetailScore(detailedScores, "button_size");
        UrlDetailReportDto.DetailScoreDto buttonContrast = extractDetailScore(detailedScores, "button_contrast");
        UrlDetailReportDto.DetailScoreDto fontSize = extractDetailScore(detailedScores, "font_size");
        UrlDetailReportDto.DetailScoreDto overallContrast = extractDetailScore(detailedScores, "overall_contrast");
        UrlDetailReportDto.DetailScoreDto koreanRatio = extractDetailScore(detailedScores, "korean_ratio");

        // 텍스트 보고서 생성
        String textReport = generateTextReport(
                analysisInfo, scrollInfo, buttonAnalysis,
                buttonDetection, buttonVisualFeedback, buttonSize, buttonContrast,
                fontSize, overallContrast, koreanRatio, summary);

        return UrlDetailReportDto.builder()
                .url(getString(analysisInfo, "url"))
                .analysisDate(getString(analysisInfo, "analysis_date"))
                .screenshotPath(getString(analysisInfo, "screenshot_path"))
                .s3Url(getString(analysisInfo, "s3_url"))
                .taskId(getString(analysisInfo, "task_id"))
                .websiteId(getString(analysisInfo, "website_id"))
                .verticalScroll(getBoolean(scrollInfo, "vertical_scroll"))
                .horizontalScroll(getBoolean(scrollInfo, "horizontal_scroll"))
                .crawledButtonCount(getInteger(buttonAnalysis, "crawled_button_count"))
                .detectedButtonCount(getInteger(buttonAnalysis, "detected_button_count"))
                .buttonCountDifference(getInteger(buttonAnalysis, "button_count_difference"))
                .buttonDetection(buttonDetection)
                .buttonVisualFeedback(buttonVisualFeedback)
                .buttonSize(buttonSize)
                .buttonContrast(buttonContrast)
                .fontSize(fontSize)
                .overallContrast(overallContrast)
                .koreanRatio(koreanRatio)
                .finalScore(report.getAccessibilityScore() != null ? report.getAccessibilityScore() : 0.0)  // 🔥 엔티티에서 직접 가져옴
                .accessibilityLevel(getString(summary, "accessibility_level"))
                .severityLevel(getString(summary, "severity_level"))
                .textReport(textReport)
                .build();
    }

    /**
     * 텍스트 보고서 생성 (사용자 제공 형식)
     */
    private String generateTextReport(
            Map<String, Object> analysisInfo,
            Map<String, Object> scrollInfo,
            Map<String, Object> buttonAnalysis,
            UrlDetailReportDto.DetailScoreDto buttonDetection,
            UrlDetailReportDto.DetailScoreDto buttonVisualFeedback,
            UrlDetailReportDto.DetailScoreDto buttonSize,
            UrlDetailReportDto.DetailScoreDto buttonContrast,
            UrlDetailReportDto.DetailScoreDto fontSize,
            UrlDetailReportDto.DetailScoreDto overallContrast,
            UrlDetailReportDto.DetailScoreDto koreanRatio,
            Map<String, Object> summary) {

        StringBuilder report = new StringBuilder();

        // 기본 정보
        report.append("보고서는 웹사이트 분석 결과, 대상 URL은 ")
                .append(getString(analysisInfo, "url"))
                .append("이며, 분석 수행 일시는 ")
                .append(getString(analysisInfo, "analysis_date"))
                .append("입니다. ");

        report.append("분석 과정에서 스크린샷은 ")
                .append(getString(analysisInfo, "screenshot_path"))
                .append("에 저장되었으며, S3에 업로드된 URL은 ")
                .append(getString(analysisInfo, "s3_url"))
                .append("입니다. ");

        report.append("해당 작업의 Task ID는 ")
                .append(getString(analysisInfo, "task_id"))
                .append("이며, 사이트 고유 식별자는 ")
                .append(getString(analysisInfo, "website_id"))
                .append("입니다.\n\n");

        // 스크롤 정보
        report.append("페이지의 스크롤 정보는 수직 스크롤 여부 ")
                .append(getBoolean(scrollInfo, "vertical_scroll"))
                .append(", 수평 스크롤 여부 ")
                .append(getBoolean(scrollInfo, "horizontal_scroll"))
                .append("로 나타났습니다. 이는 페이지 레이아웃과 정보 배치의 접근성 평가에 중요한 요소로 활용되었습니다.\n\n");

        // 버튼 분석
        report.append("버튼 분석 결과, 크롤링된 버튼 수는 ")
                .append(getInteger(buttonAnalysis, "crawled_button_count"))
                .append("개이며, AI 기반 탐지로 확인된 실제 버튼 수는 ")
                .append(getInteger(buttonAnalysis, "detected_button_count"))
                .append("개로, 두 값의 차이는 ")
                .append(getInteger(buttonAnalysis, "button_count_difference"))
                .append("개입니다. 버튼 탐지 정확도는 사용자가 페이지 내 상호작용 가능한 요소를 인식하는데 중요한 기준으로, 점수는 ")
                .append(buttonDetection.getScore())
                .append("점으로 평가되었습니다. ")
                .append(buttonDetection.getRecommendation())
                .append("\n\n");

        // 버튼 시각적 피드백
        report.append("버튼 시각적 피드백 점수는 ")
                .append(buttonVisualFeedback.getScore())
                .append("점(")
                .append(buttonVisualFeedback.getLevel())
                .append(")입니다. 이는 사용자가 버튼 클릭 시 즉시 피드백을 인지할 수 있는 정도를 평가합니다. ")
                .append(buttonVisualFeedback.getRecommendation())
                .append("\n\n");

        // 버튼 크기 및 대비
        report.append("버튼 크기와 명암 대비 점수는 각각 ")
                .append(buttonSize.getScore())
                .append("점, ")
                .append(buttonContrast.getScore())
                .append("점으로 평가되었습니다. 버튼 크기 및 대비의 적절성은 고령층 사용자가 버튼을 쉽게 인식하고 클릭할 수 있는지 여부를 판단하는 기준입니다. ")
                .append(buttonSize.getRecommendation())
                .append("\n\n");

        // 텍스트 관련
        report.append("텍스트 관련 평가 결과, 폰트 크기 점수는 ")
                .append(fontSize.getScore())
                .append("점이며, 전체 대비 점수는 ")
                .append(overallContrast.getScore())
                .append("점으로 측정되었습니다. 폰트 크기와 전체 대비 항목은 페이지 내 텍스트의 가독성과 시인성을 평가하는 핵심 요소이며, ")
                .append(fontSize.getRecommendation())
                .append("\n\n");

        // 한국어 비율
        report.append("페이지 내 한국어 텍스트 비율은 ")
                .append(koreanRatio.getScore())
                .append("점으로 나타났으며, 항목 가중치는 ")
                .append(koreanRatio.getWeight())
                .append("입니다. 이 항목은 사용자가 페이지 내용을 이해할 수 있는 핵심 기준으로 평가되었습니다. ")
                .append(koreanRatio.getRecommendation())
                .append("\n\n");

        // 종합 점수
        report.append("종합 점수는 ")
                .append(getDouble(summary, "final_score"))
                .append("점이며, 접근성 수준은 ")
                .append(getString(summary, "accessibility_level"))
                .append(", 심각도 수준은 ")
                .append(getString(summary, "severity_level"))
                .append("로 평가되었습니다.");

        return report.toString();
    }

    /**
     * 상세 점수 추출 및 권장사항 생성
     */
    private UrlDetailReportDto.DetailScoreDto extractDetailScore(Map<String, Object> detailedScores, String key) {
        Map<String, Object> scoreData = getMap(detailedScores, key);
        String level = getString(scoreData, "level");
        String recommendation = generateRecommendation(key, level);

        return UrlDetailReportDto.DetailScoreDto.builder()
                .score(getDouble(scoreData, "score"))
                .level(level)
                .weight(getDouble(scoreData, "weight"))
                .recommendation(recommendation)
                .build();
    }

    /**
     * Level에 따른 권장사항 생성
     */
    private String generateRecommendation(String category, String level) {
        if (level == null) return "";

        return switch (category) {
            case "button_detection" -> switch (level) {
                case "High" -> "버튼 탐지 수준이 우수하여 추가 개선 사항은 필요하지 않습니다.";
                case "Medium" -> "일부 버튼이 정확히 인식되지 않을 수 있으므로, OCR 모델 학습 데이터 보강 또는 크롤링 알고리즘 개선이 권장됩니다.";
                case "Low" -> "버튼 탐지 정확도가 낮아 사용자 상호작용에 어려움이 발생할 수 있습니다. OCR 및 AI 탐지 모델 전면 재검토와 레이아웃 패턴 분석 개선이 필요합니다.";
                default -> "";
            };
            case "button_visual_feedback" -> switch (level) {
                case "High" -> "버튼 피드백이 충분히 제공되고 있습니다.";
                case "Medium" -> "일부 버튼의 클릭 피드백이 미흡하므로 CSS/JS 기반 시각적 효과 개선이 권장됩니다.";
                case "Low" -> "버튼 클릭 피드백이 부족하여 사용자가 혼동할 수 있습니다. 전반적인 버튼 인터랙션 효과 구현 강화가 필요합니다.";
                default -> "";
            };
            case "button_size" -> switch (level) {
                case "High" -> "버튼 크기가 적절하게 설정되어 있습니다.";
                case "Medium" -> "일부 버튼이 최소 크기 기준보다 작아 클릭 편의성이 떨어질 수 있으므로 크기 조정이 필요합니다.";
                case "Low" -> "버튼 크기가 너무 작아 고령층 사용자가 클릭하기 어렵습니다. 버튼 전체 크기 재설계가 필요합니다.";
                default -> "";
            };
            case "font_size" -> switch (level) {
                case "High" -> "폰트 크기가 적절하여 가독성이 우수합니다.";
                case "Medium" -> "일부 텍스트의 글자 크기가 작아 가독성이 낮습니다. 주요 텍스트 크기 조정이 권장됩니다.";
                case "Low" -> "글자 크기가 너무 작아 읽기 어려움이 발생합니다. 전체 폰트 크기 상향 조정이 필요합니다.";
                default -> "";
            };
            case "korean_ratio" -> switch (level) {
                case "High" -> "한국어 텍스트 비율이 충분합니다.";
                case "Medium" -> "일부 외국어나 기호로 인해 이해도가 떨어질 수 있습니다. 한국어 안내 문구 강화가 권장됩니다.";
                case "Low" -> "한국어 텍스트 비율이 낮아 내용 이해에 어려움이 발생합니다. 페이지 전체 한국어 텍스트 확보가 필요합니다.";
                default -> "";
            };
            default -> "";
        };
    }

    // === Helper 메서드 ===

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Map ? (Map<String, Object>) value : Map.of();
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }

    private Double getDouble(Map<String, Object> map, String key) {
        if (map == null) return 0.0;
        Object value = map.get(key);

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                log.warn("Double 파싱 실패: key={}, value={}", key, value);
                return 0.0;
            }
        }
        return 0.0;
    }

    private Integer getInteger(Map<String, Object> map, String key) {
        if (map == null) return 0;
        Object value = map.get(key);

        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                log.warn("Integer 파싱 실패: key={}, value={}", key, value);
                return 0;
            }
        }
        return 0;
    }

    private Boolean getBoolean(Map<String, Object> map, String key) {
        if (map == null) return false;
        Object value = map.get(key);

        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return false;
    }

    /**
     * 모든 URL 분석 결과를 종합하여 최종 보고서 생성
     */
    public FinalReportDto generateFinalReport(String websiteUrl, String clientId, List<AccessibilityReport> reports) {
        if (reports == null || reports.isEmpty()) {
            log.warn("분석 결과가 없습니다 - websiteUrl: {}", websiteUrl);
            return createEmptyReport(websiteUrl, clientId);
        }

        log.info("최종 보고서 생성 시작 - websiteUrl: {}, 분석 URL 수: {}", websiteUrl, reports.size());

        // 각 URL별 상세 보고서 생성
        List<UrlDetailReportDto> urlReports = reports.stream()
                .map(this::generateUrlDetailReport)
                .toList();

        // 통계 계산
        FinalReportDto.StatisticsDto statistics = calculateStatistics(urlReports);

        // 평균 점수 계산
        double averageScore = urlReports.stream()
                .mapToDouble(UrlDetailReportDto::getFinalScore)
                .average()
                .orElse(0.0);

        // 전체 접근성 수준 결정
        String overallLevel = determineOverallLevel(averageScore);
        String severityLevel = determineSeverityLevel(statistics);

        // 종합 권장사항 생성
        List<String> recommendations = generateRecommendations(statistics);

        return FinalReportDto.builder()
                .websiteUrl(websiteUrl)
                .clientId(clientId)
                .totalAnalyzedUrls(reports.size())
                .averageScore(Math.round(averageScore * 100.0) / 100.0)  // 소수점 2자리
                .overallLevel(overallLevel)
                .severityLevel(severityLevel)
                .statistics(statistics)
                .urlReports(urlReports)
                .recommendations(recommendations)
                .build();
    }

    /**
     * 통계 계산
     */
    private FinalReportDto.StatisticsDto calculateStatistics(List<UrlDetailReportDto> urlReports) {
        int totalReports = urlReports.size();

        // 점수 분포 계산
        int excellentCount = 0, goodCount = 0, fairCount = 0, poorCount = 0;
        for (UrlDetailReportDto report : urlReports) {
            double score = report.getFinalScore();
            if (score >= 80) excellentCount++;
            else if (score >= 60) goodCount++;
            else if (score >= 40) fairCount++;
            else poorCount++;
        }

        return FinalReportDto.StatisticsDto.builder()
                .averageButtonDetectionScore(calculateAverageScore(urlReports, r -> r.getButtonDetection().getScore()))
                .averageButtonSizeScore(calculateAverageScore(urlReports, r -> r.getButtonSize().getScore()))
                .averageButtonContrastScore(calculateAverageScore(urlReports, r -> r.getButtonContrast().getScore()))
                .averageButtonFeedbackScore(calculateAverageScore(urlReports, r -> r.getButtonVisualFeedback().getScore()))
                .averageFontSizeScore(calculateAverageScore(urlReports, r -> r.getFontSize().getScore()))
                .averageContrastScore(calculateAverageScore(urlReports, r -> r.getOverallContrast().getScore()))
                .averageKoreanRatioScore(calculateAverageScore(urlReports, r -> r.getKoreanRatio().getScore()))
                .totalButtonsDetected(urlReports.stream().mapToInt(UrlDetailReportDto::getDetectedButtonCount).sum())
                .totalButtonsCrawled(urlReports.stream().mapToInt(UrlDetailReportDto::getCrawledButtonCount).sum())
                .excellentCount(excellentCount)
                .goodCount(goodCount)
                .fairCount(fairCount)
                .poorCount(poorCount)
                .build();
    }

    private double calculateAverageScore(List<UrlDetailReportDto> reports,
                                        java.util.function.Function<UrlDetailReportDto, Double> scoreExtractor) {
        return Math.round(reports.stream()
                .mapToDouble(scoreExtractor::apply)
                .average()
                .orElse(0.0) * 100.0) / 100.0;
    }

    /**
     * 전체 접근성 수준 결정
     */
    private String determineOverallLevel(double averageScore) {
        if (averageScore >= 80) return "Excellent";
        if (averageScore >= 60) return "Good";
        if (averageScore >= 40) return "Fair";
        return "Poor";
    }

    /**
     * 심각도 수준 결정
     */
    private String determineSeverityLevel(FinalReportDto.StatisticsDto statistics) {
        // Poor가 50% 이상이면 Critical
        int total = statistics.getExcellentCount() + statistics.getGoodCount() +
                   statistics.getFairCount() + statistics.getPoorCount();
        double poorRatio = (double) statistics.getPoorCount() / total;

        if (poorRatio >= 0.5) return "Critical";
        if (statistics.getPoorCount() > 0) return "High";
        if (statistics.getFairCount() > total * 0.3) return "Medium";
        return "Low";
    }

    /**
     * 종합 권장사항 생성
     */
    private List<String> generateRecommendations(FinalReportDto.StatisticsDto statistics) {
        List<String> recommendations = new ArrayList<>();

        // 버튼 탐지
        if (statistics.getAverageButtonDetectionScore() < 60) {
            recommendations.add("버튼 탐지 정확도가 낮습니다. OCR 모델 개선이 필요합니다.");
        }

        // 버튼 크기
        if (statistics.getAverageButtonSizeScore() < 60) {
            recommendations.add("버튼 크기가 작습니다. 최소 44x44px 이상으로 조정이 필요합니다.");
        }

        // 폰트 크기
        if (statistics.getAverageFontSizeScore() < 60) {
            recommendations.add("폰트 크기가 작습니다. 최소 14px 이상으로 조정이 권장됩니다.");
        }

        // 대비
        if (statistics.getAverageContrastScore() < 60) {
            recommendations.add("명암 대비가 부족합니다. WCAG 2.0 AA 기준(4.5:1) 이상 확보가 필요합니다.");
        }

        // 한국어 비율
        if (statistics.getAverageKoreanRatioScore() < 60) {
            recommendations.add("한국어 텍스트 비율이 낮습니다. 주요 안내 문구의 한국어 번역이 필요합니다.");
        }

        // 기본 권장사항
        if (recommendations.isEmpty()) {
            recommendations.add("전반적으로 우수한 접근성을 보이고 있습니다. 현재 수준을 유지해주세요.");
        }

        return recommendations;
    }

    /**
     * 빈 보고서 생성 (분석 결과가 없을 때)
     */
    private FinalReportDto createEmptyReport(String websiteUrl, String clientId) {
        return FinalReportDto.builder()
                .websiteUrl(websiteUrl)
                .clientId(clientId)
                .totalAnalyzedUrls(0)
                .averageScore(0.0)
                .overallLevel("Unknown")
                .severityLevel("Unknown")
                .statistics(FinalReportDto.StatisticsDto.builder().build())
                .urlReports(List.of())
                .recommendations(List.of("분석 결과가 없습니다."))
                .build();
    }
}
