package com.example.capstone_java.website.adapter.in.kafka;

import com.example.capstone_java.website.application.event.EventDispatcher;
import com.example.capstone_java.website.domain.event.UrlCrawlEvent;
import com.example.capstone_java.website.global.common.KafkaFactories;
import com.example.capstone_java.website.global.common.KafkaGroups;
import com.example.capstone_java.website.global.common.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventListener {

    private final EventDispatcher eventDispatcher;

    @KafkaListener(
        topics = KafkaTopics.URL_CRAWL_EVENTS,
        groupId = KafkaGroups.URL_PROCESSING_GROUP,
        containerFactory = KafkaFactories.URL_PROCESSING_LISTENER_CONTAINER_FACTORY
    )
    public void handleUrlCrawlEvent(UrlCrawlEvent event) {
        eventDispatcher.dispatch(event);
    }
}