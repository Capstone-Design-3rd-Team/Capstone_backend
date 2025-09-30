package com.example.capstone_java.website.adapter.out.cache;

import com.example.capstone_java.website.application.port.out.CrawlCachePort;
import com.example.capstone_java.website.domain.vo.WebsiteId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Redis를 사용한 크롤링 캐시 어댑터
 *
 * 주요 기능:
 * 1. URL 중복 체크를 위한 고속 캐시
 * 2. 배치 중복 체크로 DB 부하 감소
 * 3. TTL을 통한 자동 캐시 만료
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCrawlCacheAdapter implements CrawlCachePort {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String KEY_PREFIX = "crawled_urls:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24); // 24시간 TTL

    @Override
    public boolean isUrlCached(WebsiteId websiteId, String url) {
        String key = buildKey(websiteId);
        SetOperations<String, String> setOps = redisTemplate.opsForSet();
        Boolean isCached = setOps.isMember(key, url);
        return Boolean.TRUE.equals(isCached);
    }

    @Override
    public void cacheUrl(WebsiteId websiteId, String url) {
        String key = buildKey(websiteId);
        SetOperations<String, String> setOps = redisTemplate.opsForSet();

        setOps.add(key, url);
        redisTemplate.expire(key, DEFAULT_TTL);

        log.debug("URL 캐시 저장: WebsiteId={}, URL={}", websiteId.getId(), url);
    }

    @Override
    public void cacheUrls(WebsiteId websiteId, List<String> urls) {
        if (urls.isEmpty()) {
            return;
        }

        String key = buildKey(websiteId);
        SetOperations<String, String> setOps = redisTemplate.opsForSet();

        // 배치로 캐시에 추가
        String[] urlArray = urls.toArray(new String[0]);
        setOps.add(key, urlArray);
        redisTemplate.expire(key, DEFAULT_TTL);

        log.info("URL 배치 캐시 저장: WebsiteId={}, URL 개수={}", websiteId.getId(), urls.size());
    }

    @Override
    public Set<String> filterCachedUrls(WebsiteId websiteId, List<String> urls) {
        String key = buildKey(websiteId);
        SetOperations<String, String> setOps = redisTemplate.opsForSet();

        return urls.stream()
            .filter(url -> Boolean.TRUE.equals(setOps.isMember(key, url)))
            .collect(Collectors.toSet());
    }

    @Override
    public Set<String> filterNewUrls(WebsiteId websiteId, List<String> urls) {
        String key = buildKey(websiteId);
        SetOperations<String, String> setOps = redisTemplate.opsForSet();

        return urls.stream()
            .filter(url -> !Boolean.TRUE.equals(setOps.isMember(key, url)))
            .collect(Collectors.toSet());
    }

    @Override
    public void clearCache(WebsiteId websiteId) {
        String key = buildKey(websiteId);
        redisTemplate.delete(key);
        log.info("캐시 삭제: WebsiteId={}", websiteId.getId());
    }

    @Override
    public long getCacheSize(WebsiteId websiteId) {
        String key = buildKey(websiteId);
        SetOperations<String, String> setOps = redisTemplate.opsForSet();
        Long size = setOps.size(key);
        return size != null ? size : 0L;
    }

    private String buildKey(WebsiteId websiteId) {
        return KEY_PREFIX + websiteId.getId();
    }
}