package com.example.capstone_java.website.application.event;

import com.example.capstone_java.website.domain.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 트랜잭션 경계와 도메인 이벤트 디스패칭을 연결하는 브릿지
 *
 * 역할:
 * - Spring의 @TransactionalEventListener로 트랜잭션 커밋 후 이벤트 수신
 * - 기존 EventDispatcher로 도메인 이벤트 전달
 *
 * 목적:
 * - DB 커밋 전에 Kafka 메시지가 발행되는 Race Condition 방지
 * - 기존 도메인 이벤트 아키텍처(EventDispatcher/EventHandler) 유지
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionalEventBridge {

    private final EventDispatcher eventDispatcher;

    /**
     * 트랜잭션 커밋 후에 도메인 이벤트를 기존 EventDispatcher로 전달
     *
     * @param event 도메인 이벤트 (ExtractionStartedEvent, UrlCrawlEvent 등)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDomainEventAfterCommit(DomainEvent event) {
        log.debug("트랜잭션 커밋 완료 - 도메인 이벤트 디스패치 시작: {}", event.getClass().getSimpleName());
        eventDispatcher.dispatch(event);
    }
}
