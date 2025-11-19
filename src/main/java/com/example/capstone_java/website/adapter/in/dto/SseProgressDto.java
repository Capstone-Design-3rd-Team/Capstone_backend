package com.example.capstone_java.website.adapter.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * SSE 진행 상황 DTO (순수 데이터 전송 역할)
 *
 * stage 값:
 * - CRAWLING: URL 수집 중
 * - ANALYZING: AI 분석 중
 * - COMPLETED: 완료
 * - ERROR: 에러
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "실시간 분석 진행 상황 데이터 (event: progress)")
public class SseProgressDto {
    @Schema(
            description = "현재 진행 단계 (프론트 UI 분기 기준)",
            example = "CRAWLING",
            allowableValues = {"CRAWLING", "ANALYZING", "COMPLETED", "ERROR"}
    )
    private String stage;

    @Schema(description = "수집된 URL 개수 (CRAWLING 단계에서 사용)", example = "45")
    private Integer crawledCount;

    @Schema(description = "분석 완료된 URL 개수 (ANALYZING 단계에서 사용)", example = "60")
    private Integer analyzedCount;

    @Schema(description = "전체 URL 개수", example = "100")
    private Integer totalCount;

    @Schema(description = "진행률 (%) (ANALYZING 단계에서 ProgressBar로 표시)", example = "60")
    private Integer percentage;

    @Schema(description = "사용자 표시 메시지", example = "URL 수집 중... 45개 발견")
    private String message;
}
