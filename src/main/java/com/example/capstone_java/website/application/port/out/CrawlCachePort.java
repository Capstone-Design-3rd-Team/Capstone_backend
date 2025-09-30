package com.example.capstone_java.website.application.port.out;

import com.example.capstone_java.website.domain.vo.WebsiteId;

import java.util.List;
import java.util.Set;

/**
 * 크롤링 URL 캐시 관리를 위한 포트
 *
 * Redis를 통한 고속 중복 체크와 성능 최적화를 제공
 */
public interface CrawlCachePort {

    /**
     * 특정 URL이 이미 캐시에 있는지 확인
     *
     * @param websiteId 웹사이트 ID
     * @param url 확인할 URL
     * @return 캐시에 있으면 true, 없으면 false
     */
    boolean isUrlCached(WebsiteId websiteId, String url);

    /**
     * 단일 URL을 캐시에 저장
     *
     * @param websiteId 웹사이트 ID
     * @param url 캐시할 URL
     */
    void cacheUrl(WebsiteId websiteId, String url);

    /**
     * 여러 URL을 배치로 캐시에 저장
     *
     * @param websiteId 웹사이트 ID
     * @param urls 캐시할 URL 목록
     */
    void cacheUrls(WebsiteId websiteId, List<String> urls);

    /**
     * URL 목록에서 이미 캐시된 URL들만 필터링하여 반환
     *
     * @param websiteId 웹사이트 ID
     * @param urls 확인할 URL 목록
     * @return 캐시된 URL들의 Set
     */
    Set<String> filterCachedUrls(WebsiteId websiteId, List<String> urls);

    /**
     * URL 목록에서 캐시되지 않은 새로운 URL들만 필터링하여 반환
     *
     * @param websiteId 웹사이트 ID
     * @param urls 확인할 URL 목록
     * @return 새로운 URL들의 Set
     */
    Set<String> filterNewUrls(WebsiteId websiteId, List<String> urls);

    /**
     * 특정 웹사이트의 모든 캐시 삭제
     *
     * @param websiteId 웹사이트 ID
     */
    void clearCache(WebsiteId websiteId);

    /**
     * 캐시된 URL 개수 조회
     *
     * @param websiteId 웹사이트 ID
     * @return 캐시된 URL 개수
     */
    long getCacheSize(WebsiteId websiteId);
}