package com.example.capstone_java.website.global.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * SSE 하트비트 전송
 *
 * 책임: 주기적으로 연결 유지 신호 전송 (AWS LB 타임아웃 방지)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseHeartbeatScheduler {

    private final SseEmitters sseEmitters;

    /**
     * 45초마다 하트비트 전송 (AWS LB 60초 타임아웃 방지)
     */
    @Scheduled(fixedRate = 45000)
    public void sendHeartbeat() {
        sseEmitters.getAll().forEach((clientId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
            } catch (IOException e) {
                log.debug("하트비트 실패, 연결 제거: {}", clientId);
                sseEmitters.complete(clientId);
            }
        });
    }
}
