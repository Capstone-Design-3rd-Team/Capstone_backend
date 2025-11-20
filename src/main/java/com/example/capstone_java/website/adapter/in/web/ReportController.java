package com.example.capstone_java.website.adapter.in.web;

import com.example.capstone_java.website.adapter.in.dto.FinalReportDto;
import com.example.capstone_java.website.application.port.out.GetFinalReportPort;
import com.example.capstone_java.website.domain.vo.WebsiteId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 최종 보고서 조회 API
 *
 * 책임: 완료된 분석 보고서를 프론트엔드에 제공
 */
@Slf4j
@Tag(name = "Report", description = "분석 결과 보고서 API")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final GetFinalReportPort getFinalReportPort;

    /**
     * 최종 보고서 조회
     *
     * SSE로 complete 신호를 받은 후 호출하는 API
     */
    @Operation(
            summary = "최종 분석 보고서 조회",
            description = """
            SSE로 complete 이벤트를 받은 후 호출하여 최종 보고서를 가져옵니다.

            **플로우:**
            1. SSE 연결: GET /api/sse/connect/{clientId}
            2. progress 이벤트 수신 (크롤링 → 분석 진행)
            3. complete 이벤트 수신: {"websiteId": "...", "status": "COMPLETED"}
            4. 이 API 호출: GET /api/reports/{websiteId}
            5. 최종 보고서 다운로드

            **장점:**
            - SSE 안정성 향상 (작은 신호만 전송)
            - 네트워크 오류 시 재시도 가능
            - 언제든지 재다운로드 가능
            """
    )
    @ApiResponse(
            responseCode = "200",
            description = "최종 보고서 조회 성공",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = FinalReportDto.class)
            )
    )
    @ApiResponse(responseCode = "404", description = "보고서를 찾을 수 없음 (아직 분석 완료 안 됨)")
    @GetMapping("/{websiteId}")
    public ResponseEntity<FinalReportDto> getFinalReport(
            @Parameter(description = "웹사이트 ID (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String websiteId) {

        log.info("최종 보고서 조회 요청: websiteId={}", websiteId);

        UUID uuid = UUID.fromString(websiteId);
        WebsiteId id = WebsiteId.of(uuid);

        return getFinalReportPort.findByWebsiteId(id)
                .map(report -> {
                    log.info("최종 보고서 조회 성공: websiteId={}, urls={}, score={}",
                            websiteId,
                            report.getUrlReports() != null ? report.getUrlReports().size() : 0,
                            report.getAverageScore());
                    return ResponseEntity.ok(report);
                })
                .orElseGet(() -> {
                    log.warn("최종 보고서 없음: websiteId={}", websiteId);
                    return ResponseEntity.notFound().build();
                });
    }
}
