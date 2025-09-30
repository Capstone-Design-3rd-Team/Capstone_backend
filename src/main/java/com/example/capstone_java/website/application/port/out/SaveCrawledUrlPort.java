package com.example.capstone_java.website.application.port.out;

import com.example.capstone_java.website.domain.entity.CrawledUrl;
import com.example.capstone_java.website.domain.vo.WebsiteId;

import java.util.List;
import java.util.Set;

/**
 * 크롤링된 URL 정보를 저장하고 조회하는 포트
 *
 * 배치 처리와 중복 체크 최적화를 위한 메서드들을 제공
 */
public interface SaveCrawledUrlPort {

    /**
     * 단일 CrawledUrl 저장
     */
    CrawledUrl save(CrawledUrl crawledUrl);

    /**
     * 여러 CrawledUrl을 배치로 저장
     * 배치 처리로 DB 성능 최적화
     */
    List<CrawledUrl> saveAll(List<CrawledUrl> crawledUrls);

    /**
     * 지정된 WebsiteId와 URL 목록에서 이미 존재하는 URL들을 반환
     * 배치 중복 체크를 위한 최적화된 쿼리
     *
     * @param websiteId 웹사이트 ID
     * @param urls 확인할 URL 목록
     * @return 이미 존재하는 URL들의 Set
     */
    Set<String> findExistingUrls(WebsiteId websiteId, List<String> urls);

    /**
     * 특정 WebsiteId와 URL로 CrawledUrl 존재 여부 확인
     */
    boolean existsByWebsiteIdAndUrl(WebsiteId websiteId, String url);

    /**
     * 특정 WebsiteId의 크롤링 대상 URL들 조회 (DISCOVERED 상태)
     */
    List<CrawledUrl> findPendingUrls(WebsiteId websiteId, int maxDepth);
}