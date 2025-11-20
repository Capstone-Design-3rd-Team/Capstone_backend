package com.example.capstone_java.website.adapter.out.persistence.repository;

import com.example.capstone_java.website.adapter.out.persistence.entity.CrawledUrlEntity;
import com.example.capstone_java.website.domain.entity.CrawlStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface CrawledUrlJpaRepository extends JpaRepository<CrawledUrlEntity, Long> {

    /**
     * 특정 웹사이트의 URL들 중에서 이미 존재하는 URL들 찾기
     */
    @Query("SELECT c.url FROM CrawledUrlEntity c WHERE c.websiteId = :websiteId AND c.url IN :urls")
    Set<String> findExistingUrlsByWebsiteIdAndUrls(@Param("websiteId") UUID websiteId, @Param("urls") List<String> urls);

    /**
     * 특정 웹사이트의 모든 크롤링된 URL들 조회
     */
    List<CrawledUrlEntity> findByWebsiteIdOrderByDiscoveredAtDesc(UUID websiteId);

    /**
     * 특정 웹사이트와 URL로 중복 체크
     */
    boolean existsByWebsiteIdAndUrl(UUID websiteId, String url);

    /**
     * 특정 웹사이트의 크롤링된 URL 총 개수 조회
     */
    long countByWebsiteId(UUID websiteId);

    // [추가] 특정 상태(FAILED)인 URL 개수 조회
    long countByWebsiteIdAndStatus(UUID websiteId, CrawlStatus status);
}