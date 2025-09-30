package com.example.capstone_java.website.global.common;

public final class KafkaTopics {
    public static final String EXTRACTION_STARTED_EVENTS = "extraction-started-events";
    public static final String URL_CRAWL_EVENTS = "url-crawl-events";
    public static final String URL_DISCOVERED_EVENTS = "url-discovered-events";
    public static final String ACCESSIBILITY_JUDGED_EVENTS = "accessibility-judged-events";
    public static final String FAILED_EVENTS_DLQ = "failed-events-dlq";

    private KafkaTopics() {
        throw new AssertionError("상수 클래스는 인스턴스 생성 불가");
    }
}
