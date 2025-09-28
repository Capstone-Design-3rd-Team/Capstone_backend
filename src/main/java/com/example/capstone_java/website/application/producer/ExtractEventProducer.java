package com.example.capstone_java.website.application.producer;

import com.example.capstone_java.website.global.common.KafkaTopics;
import com.example.capstone_java.website.global.common.PublishEventPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExtractEventProducer implements PublishEventPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(Object event) {
        try {
           kafkaTemplate.send(KafkaTopics.EXTRACTION_STARTED_EVENTS, event);
        }catch (Exception e) {
            log.error("이벤트 발행 실패 - Event: {}, Error: {}", event.getClass().getSimpleName(), e.getMessage(), e);
        }
    }
}
