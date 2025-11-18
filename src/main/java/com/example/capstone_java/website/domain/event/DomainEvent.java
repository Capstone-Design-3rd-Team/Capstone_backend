package com.example.capstone_java.website.domain.event;

import java.time.LocalDateTime;

/**
 * 도메인 이벤트의 기본 인터페이스
 * 모든 도메인 이벤트는 이 인터페이스를 구현해야 함
 */
public interface DomainEvent {

    /**
     * 이벤트 발생 시각
     */
    LocalDateTime occurredAt();

    /**
     * 이벤트 타입 반환
     */
    default String getEventType() {
        return this.getClass().getSimpleName();
    }

    /**
     * Kafka 파티셔닝을 위한 키 생성
     * 기본값은 null → 라운드 로빈 분산 (순서 보장 불필요)
     * 순서 보장이 필요한 경우에만 오버라이드
     */
    default String getPartitionKey() {
        return null;
    }
}