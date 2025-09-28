package com.example.capstone_java.website.global.common;

public final class KafkaFactories {
    public static final String EXTRACTION_LISTENER_CONTAINER_FACTORY = "extractionListenerContainerFactory";
    public static final String URL_PROCESSING_LISTENER_CONTAINER_FACTORY = "urlProcessingListenerContainerFactory";
    public static final String AI_RESULT_LISTENER_CONTAINER_FACTORY = "aiResultListenerContainerFactory";
    public static final String FAILURE_LISTENER_CONTAINER_FACTORY = "failureListenerContainerFactory";
    public static final String DEFAULT_KAFKA_LISTENER_CONTAINER_FACTORY = "kafkaListenerContainerFactory";

    private KafkaFactories() {
        throw new AssertionError("상수 클래스는 인스턴스 생성 불가");
    }

}
