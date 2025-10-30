package com.example.capstone_java.website.domain.event;

/**
 * 도메인 이벤트 처리를 담당하는 핸들러 인터페이스
 * @param <E> 처리할 도메인 이벤트 타입
 */
public interface EventHandler<E extends DomainEvent> {

    /**
     * 해당 이벤트를 처리할 수 있는지 확인
     * @param event 확인할 도메인 이벤트
     * @return 처리 가능 여부
     */
    boolean supports(DomainEvent event);

    /**
     * 이벤트 처리
     * @param event 처리할 도메인 이벤트
     */
    void handle(E event);
}