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
     * URL 크롤링 이벤트 토픽 - m7i-flex.large (2 vCPU) 최적화
     */
    @Bean
    public NewTopic urlCrawlEventsTopic() {
        return TopicBuilder.name(KafkaTopics.URL_CRAWL_EVENTS)
                .partitions(6)  // concurrency 6 (Playwright 네트워크 I/O 대기)
                .replicas(1)
                .build();
    }

    /**
     * URL 발견 이벤트 토픽 - m7i-flex.large (2 vCPU) 최적화
     */
    @Bean
    public NewTopic urlDiscoveredEventsTopic() {
        return TopicBuilder.name(KafkaTopics.URL_DISCOVERED_EVENTS)
                .partitions(3)  // concurrency 3 (DB+Redis 배치 I/O)
                .replicas(1)
                .build();
    }

    /**
     * 추출 시작 이벤트 토픽 - m7i-flex.large (2 vCPU) 최적화
     */
    @Bean
    public NewTopic extractionStartedEventsTopic() {
        return TopicBuilder.name(KafkaTopics.EXTRACTION_STARTED_EVENTS)
                .partitions(1)  // concurrency 1 (최초 시작 이벤트, 빈도 낮음)
                .replicas(1)
                .build();
    }

    /**
     * AI 분석 요청 이벤트 토픽 - m7i-flex.large (2 vCPU) 최적화
     */
    @Bean
    public NewTopic urlAnalysisRequestEventsTopic() {
        return TopicBuilder.name(KafkaTopics.URL_ANALYSIS_REQUEST_EVENTS)
                .partitions(4)  // concurrency 4 (가벼운 HTTP 요청)
                .replicas(1)
                .build();
    }

    /**
     * 접근성 판정 결과 토픽 - m7i-flex.large (2 vCPU) 최적화
     */
    @Bean
    public NewTopic accessibilityJudgedEventsTopic() {
        return TopicBuilder.name(KafkaTopics.ACCESSIBILITY_JUDGED_EVENTS)
                .partitions(3)  // concurrency 3 (DB 저장 I/O)
                .replicas(1)
                .build();
    }
}
