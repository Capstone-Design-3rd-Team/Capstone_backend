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
     */
    @Scheduled(fixedRate = 45000)
    public void sendHeartbeat() {
        Set<String> clientIds = sseEmitters.getAllClientIds();
        List<String> failedClients = new ArrayList<>();

        // 1단계: 하트비트 전송 및 실패한 클라이언트 수집
        for (String clientId : clientIds) {
            if (!sseEmitters.sendHeartbeat(clientId)) {
                failedClients.add(clientId);
            }
        }

        // 2단계: 실패한 연결들을 안전하게 제거
        if (!failedClients.isEmpty()) {
            log.debug("하트비트 실패한 연결 제거: {} 개", failedClients.size());
            failedClients.forEach(sseEmitters::complete);
        }
    }
}
