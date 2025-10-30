package com.example.capstone_java.website.global.config;

import com.example.capstone_java.website.global.common.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "1");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * URL 크롤링 이벤트 토픽 - 병렬 처리를 위해 8개 파티션 설정
     */
    @Bean
    public NewTopic urlCrawlEventsTopic() {
        return TopicBuilder.name(KafkaTopics.URL_CRAWL_EVENTS)
                .partitions(8)
                .replicas(1)
                .build();
    }

    /**
     * URL 발견 이벤트 토픽 - 병렬 처리를 위해 4개 파티션 설정
     */
    @Bean
    public NewTopic urlDiscoveredEventsTopic() {
        return TopicBuilder.name(KafkaTopics.URL_DISCOVERED_EVENTS)
                .partitions(4)
                .replicas(1)
                .build();
    }

    /**
     * 추출 시작 이벤트 토픽 - 순차 처리용 1개 파티션
     */
    @Bean
    public NewTopic extractionStartedEventsTopic() {
        return TopicBuilder.name(KafkaTopics.EXTRACTION_STARTED_EVENTS)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
