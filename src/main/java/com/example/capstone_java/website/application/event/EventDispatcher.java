package com.example.capstone_java.website.application.event;

import com.example.capstone_java.website.domain.event.DomainEvent;
import com.example.capstone_java.website.domain.event.EventHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EventDispatcher {
    //spring이 자동으로 bean으로 등록된 handler들을 찾아서 넣어줌
    private final List<EventHandler<? extends DomainEvent>> handlers;

    @SuppressWarnings("unchecked")
    public void dispatch(DomainEvent event) {
        for (EventHandler<? extends DomainEvent> handler : handlers) {
            if (handler.supports(event)) {
                ((EventHandler<DomainEvent>) handler).handle(event);
            }
        }
    }
}