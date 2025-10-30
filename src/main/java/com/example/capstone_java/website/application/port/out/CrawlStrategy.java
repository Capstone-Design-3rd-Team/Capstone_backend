package com.example.capstone_java.website.application.port.out;

import java.util.List;

/**
 * 크롤링 전략 인터페이스
 *
 * Strategy Pattern을 적용하여 다양한 크롤링 방식 지원
 * - JsoupStrategy: 정적 HTML 크롤링 (빠름)
 * - PlaywrightStrategy: 동적 JavaScript 크롤링 (느림, SPA 지원)
 */
public interface CrawlStrategy {

    /**
     * URL에서 링크 추출
     *
     * @param url 크롤링할 URL
     * @return 발견된 URL 목록
     */
    List<String> extractUrls(String url);

    /**
     * 이 전략이 주어진 URL을 처리할 수 있는지 판단
     *
     * @param url 확인할 URL
     * @return 처리 가능 여부
     */
    boolean canHandle(String url);

    /**
     * 전략의 우선순위 (낮을수록 먼저 시도)
     *
     * @return 우선순위
     */
    int getPriority();

    /**
     * 전략 이름
     */
    String getName();
}
