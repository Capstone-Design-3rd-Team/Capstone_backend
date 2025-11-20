package com.example.capstone_java.website.application.port.out;

import com.example.capstone_java.website.domain.vo.WebsiteId;

/**
 * CrawledUrl 조회 Port
 */
public interface GetCrawledUrlPort {

    /**
     * WebsiteId로 크롤링된 URL 개수 조회 (모든 상태 포함)
     */
    long countByWebsiteId(WebsiteId websiteId);

    /**
     * WebsiteId로 크롤링 성공한 URL 개수 조회 (status = CRAWLED만)
     *
     * 용도: AI 분석 진행률 계산 시 사용
     * - AI 분석은 CRAWLED 상태의 URL에 대해서만 수행됨
     * - totalAnalyzed / totalCrawled 계산 시 정확한 분모 값 제공
     */
    long countCrawledUrls(WebsiteId websiteId);

    /**
     * 실패한 URL 개수 조회 (status = FAILED만)
     */
    long countFailedByWebsiteId(WebsiteId websiteId);
}
