package com.example.capstone_java.website.application.event.handler;

import com.example.capstone_java.website.domain.event.DomainEvent;
import com.example.capstone_java.website.domain.event.EventHandler;
import com.example.capstone_java.website.domain.event.ExtractionStartedEvent;
import com.example.capstone_java.website.global.common.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExtractionStartedEventHandler implements EventHandler<ExtractionStartedEvent> {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof ExtractionStartedEvent;
    }

    @Override
    public void handle(ExtractionStartedEvent event) {
        kafkaTemplate.send(KafkaTopics.EXTRACTION_STARTED_EVENTS, event.getPartitionKey(), event);
    }
}