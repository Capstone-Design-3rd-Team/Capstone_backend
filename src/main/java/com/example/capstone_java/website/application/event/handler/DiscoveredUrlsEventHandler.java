package com.example.capstone_java.website.application.event.handler;

import com.example.capstone_java.website.domain.event.DiscoveredUrlsEvent;
import com.example.capstone_java.website.domain.event.DomainEvent;
import com.example.capstone_java.website.domain.event.EventHandler;
import com.example.capstone_java.website.global.common.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DiscoveredUrlsEventHandler implements EventHandler<DiscoveredUrlsEvent> {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof DiscoveredUrlsEvent;
    }

    @Override
    public void handle(DiscoveredUrlsEvent event) {
        kafkaTemplate.send(KafkaTopics.URL_DISCOVERED_EVENTS, event.getPartitionKey(), event);
    }
}