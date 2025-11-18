package com.example.capstone_java.website.global.common;

public final class KafkaGroups {
    public static final String WEBSITE_EXTRACTION_GROUP = "website-extraction-group";
    public static final String URL_PROCESSING_GROUP = "url-processing-group";
    public static final String JOB_UPDATING_GROUP = "job-updating-group";
    public static final String AI_ANALYSIS_REQUEST_GROUP = "ai-analysis-request-group";
    public static final String AI_JUDGMENT_GROUP = "ai-judgment-group";
    public static final String FAILED_EVENTS_GROUP = "failed-events-group";

    private KafkaGroups() {
        throw new AssertionError("상수 클래스는 인스턴스 생성 불가");
    }
}
