package com.example.capstone_java.website.application.port.out;

import com.example.capstone_java.website.domain.vo.WebsiteId;

/**
 * CrawledUrl 조회 Port
 */
public interface GetCrawledUrlPort {

    /**
     * WebsiteId로 크롤링된 URL 개수 조회
     */
    long countByWebsiteId(WebsiteId websiteId);

    // [추가] 실패한 URL 개수 조회
    long countFailedByWebsiteId(WebsiteId websiteId);
}
