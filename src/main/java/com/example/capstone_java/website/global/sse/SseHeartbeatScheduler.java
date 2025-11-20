package com.example.capstone_java.website.global.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
     * 병렬 처리(parallelStream)를 통해 많은 연결도 빠르게 처리
     */
    @Scheduled(fixedRate = 45000)
    public void sendHeartbeat() {
        Set<String> clientIds = sseEmitters.getAllClientIds();

        if (clientIds.isEmpty()) return;

        // [수정] parallelStream()을 사용하여 병렬로 빠르게 전송
        // (기존에는 순차적으로 보내서 연결이 많아지면 느려질 수 있었음)
        clientIds.parallelStream().forEach(clientId -> {
            boolean success = sseEmitters.sendHeartbeat(clientId);
            if (!success) {
                log.debug("하트비트 실패, 연결 제거: {}", clientId);
                sseEmitters.complete(clientId);
            }
        });
    }
}
