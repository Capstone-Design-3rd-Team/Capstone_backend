package com.example.capstone_java.website.global.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 연결 관리
 *
 * 책임: 연결의 생성, 저장, 조회, 삭제, 데이터 전송
 */
@Slf4j
@Component
public class SseEmitters {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private static final Long TIMEOUT = 24 * 60 * 60 * 1000L; // 24시간

    /**
     * 연결 생성 및 저장
     */
    public SseEmitter add(String clientId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        emitters.put(clientId, emitter);

        emitter.onCompletion(() -> remove(clientId));
        emitter.onTimeout(() -> remove(clientId));
        emitter.onError((e) -> remove(clientId));

        log.info("SSE 연결 생성: {}", clientId);
        return emitter;
    }

    /**
     * 데이터 전송
     */
    public void send(String clientId, Object data, String eventName) {
        SseEmitter emitter = emitters.get(clientId);
        if (emitter == null) {
            log.warn("SSE 연결 없음: {}", clientId);
            return;
        }

        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
            log.debug("SSE 전송: clientId={}, event={}", clientId, eventName);
        } catch (IOException e) {
            log.error("SSE 전송 실패: {}", clientId);
            remove(clientId);
        }
    }

    /**
     * 연결 명시적 종료
     */
    public void complete(String clientId) {
        SseEmitter emitter = emitters.remove(clientId);
        if (emitter != null) {
            try {
                emitter.complete();
                log.info("SSE 연결 종료: {}", clientId);
            } catch (Exception e) {
                log.debug("SSE 연결 종료 중 예외 (이미 끊긴 연결): clientId={}, error={}", clientId, e.getMessage());
            }
        }
    }

    /**
     * 연결 제거
     */
    private void remove(String clientId) {
        emitters.remove(clientId);
        log.debug("SSE 연결 제거: {}", clientId);
    }

    /**
     * 모든 clientId 조회 (하트비트용)
     * 실제 맵 대신 불변 Set을 반환하여 안전성 보장
     */
    public Set<String> getAllClientIds() {
        return Collections.unmodifiableSet(emitters.keySet());
    }

    /**
     * 특정 clientId에 하트비트 전송 (하트비트 스케줄러 전용)
     * @return 성공 여부
     */
    public boolean sendHeartbeat(String clientId) {
        SseEmitter emitter = emitters.get(clientId);
        if (emitter == null) {
            return false;
        }

        try {
            emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
            return true;
        } catch (IOException e) {
            log.debug("하트비트 전송 실패: {}", clientId);
            return false;
        }
    }
}
