package com.example.capstone_java.website.adapter.in.web;

import com.example.capstone_java.website.adapter.in.dto.FinalReportDto;
import com.example.capstone_java.website.adapter.in.dto.SseProgressDto;
import com.example.capstone_java.website.global.sse.SseEmitters;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * SSE 연결 진입점
 *
 * 책임: HTTP 요청을 받아 SSE 연결 생성
 */
@Tag(name = "SSE Notification", description = "실시간 진행상황 및 결과 알림")
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseEmitters sseEmitters;
    /**
     * SSE 연결 생성
     */
    @Operation(
            summary = "분석 진행상황 구독 (SSE 연결)",
            description = """
            서버와 SSE(Server-Sent Events) 연결을 맺습니다.
            
            **[이벤트 흐름]**
            1. **연결 성공 시**: event=`connect`, data="connected"
            2. **크롤링 중**: event=`progress`, data=`SseProgressDto` (stage="CRAWLING") -> **URL 개수만 표시**
            3. **분석 중**: event=`progress`, data=`SseProgressDto` (stage="ANALYZING") -> **퍼센트(%) 표시**
            4. **완료 시**: event=`complete`, data=`FinalReportDto` -> **최종 결과 리포트**
            """
    )
    @ApiResponse(
            responseCode = "200",
            description = "SSE 스트림 연결 성공 (데이터가 지속적으로 전송됨)",
            content = @Content(
                    mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                    schema = @Schema(
                            description = "진행 상황(SseProgressDto) 또는 최종 결과(FinalReportDto)가 전송됩니다.",
                            oneOf = {SseProgressDto.class, FinalReportDto.class}
                    )
            )
    )
    /**
     * SSE 연결 생성
     *
     * @param clientId 클라이언트 ID (프론트에서 생성)
     * @return SseEmitter
     */
    @GetMapping(value = "/connect/{clientId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(
            @Parameter(description = "웹사이트 등록 시 발급받은 클라이언트 ID", example = "client_12345")
            @PathVariable String clientId,
            HttpServletResponse response) throws IOException {

        // SSE 필수 헤더 명시적 설정
        response.setHeader("Content-Type", "text/event-stream;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");  // Nginx 프록시용

        SseEmitter emitter = sseEmitters.add(clientId);

        // 연결 직후 더미 데이터 전송 (연결 즉시 성립)
        emitter.send(SseEmitter.event().name("connect").data("connected"));

        return emitter;
    }
}
