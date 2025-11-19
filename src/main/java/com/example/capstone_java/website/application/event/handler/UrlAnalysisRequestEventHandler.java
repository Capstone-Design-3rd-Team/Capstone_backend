package com.example.capstone_java.website.application.event.handler;

import com.example.capstone_java.website.domain.event.DomainEvent;
import com.example.capstone_java.website.domain.event.EventHandler;
import com.example.capstone_java.website.domain.event.UrlAnalysisRequestEvent;
import com.example.capstone_java.website.global.common.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UrlAnalysisRequestEventHandler implements EventHandler<UrlAnalysisRequestEvent> {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof UrlAnalysisRequestEvent;
    }

    @Override
    public void handle(UrlAnalysisRequestEvent event) {
        kafkaTemplate.send(KafkaTopics.URL_ANALYSIS_REQUEST_EVENTS, event.getPartitionKey(), event);
    }
}
