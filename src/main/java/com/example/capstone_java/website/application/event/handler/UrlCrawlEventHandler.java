package com.example.capstone_java.website.application.event.handler;

import com.example.capstone_java.website.domain.event.DomainEvent;
import com.example.capstone_java.website.domain.event.EventHandler;
import com.example.capstone_java.website.domain.event.UrlCrawlEvent;
import com.example.capstone_java.website.global.common.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UrlCrawlEventHandler implements EventHandler<UrlCrawlEvent> {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof UrlCrawlEvent;
    }

    @Override
    public void handle(UrlCrawlEvent event) {
        kafkaTemplate.send(KafkaTopics.URL_CRAWL_EVENTS, event.getPartitionKey(), event);
    }
}