package com.example.capstone_java.website.domain.exception;

/**
 * 중복 분석 요청 예외
 */
public class DuplicateAnalysisException extends RuntimeException {

    public DuplicateAnalysisException(String message) {
        super(message);
    }

    public static DuplicateAnalysisException inProgress(String clientId, String url) {
        return new DuplicateAnalysisException(
            String.format("이미 분석 중입니다. (clientId: %s, url: %s)", clientId, url)
        );
    }
}
