package com.example.capstone_java.website.global.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
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
            emitter.complete();
            log.info("SSE 연결 종료: {}", clientId);
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
     * 모든 연결 조회 (하트비트용)
     */
    public Map<String, SseEmitter> getAll() {
        return emitters;
    }
}
