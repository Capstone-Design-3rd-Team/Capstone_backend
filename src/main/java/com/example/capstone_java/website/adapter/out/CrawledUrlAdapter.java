package com.example.capstone_java.website.adapter.out;

import com.example.capstone_java.website.adapter.out.mapper.CrawledUrlMapper;
import com.example.capstone_java.website.adapter.out.persistence.entity.CrawledUrlEntity;
import com.example.capstone_java.website.adapter.out.persistence.repository.CrawledUrlJpaRepository;
import com.example.capstone_java.website.application.port.out.GetCrawledUrlPort;
import com.example.capstone_java.website.application.port.out.SaveCrawledUrlPort;
import com.example.capstone_java.website.domain.entity.CrawlStatus;
import com.example.capstone_java.website.domain.entity.CrawledUrl;
import com.example.capstone_java.website.domain.vo.WebsiteId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrawledUrlAdapter implements SaveCrawledUrlPort, GetCrawledUrlPort {

    private final CrawledUrlJpaRepository crawledUrlRepository;
    private final CrawledUrlMapper crawledUrlMapper;

    @Override
    public List<CrawledUrl> saveAll(List<CrawledUrl> crawledUrls) {
        List<CrawledUrlEntity> entities = crawledUrls.stream()
                .map(crawledUrlMapper::toEntity)
                .collect(Collectors.toList());

        List<CrawledUrlEntity> savedEntities = crawledUrlRepository.saveAll(entities);
        log.debug("크롤링된 URL {} 개 저장 완료", savedEntities.size());

        return savedEntities.stream()
                .map(crawledUrlMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Set<String> findExistingUrls(WebsiteId websiteId, List<String> urls) {
        return crawledUrlRepository.findExistingUrlsByWebsiteIdAndUrls(
                websiteId.getId(),
                urls
        );
    }

    @Override
    public List<CrawledUrl> findByWebsiteId(WebsiteId websiteId) {
        List<CrawledUrlEntity> entities = crawledUrlRepository.findByWebsiteIdOrderByDiscoveredAtDesc(
                websiteId.getId()
        );

        return entities.stream()
                .map(crawledUrlMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public CrawledUrl save(CrawledUrl crawledUrl) {
        CrawledUrlEntity entity = crawledUrlMapper.toEntity(crawledUrl);
        CrawledUrlEntity savedEntity = crawledUrlRepository.save(entity);
        return crawledUrlMapper.toDomain(savedEntity);
    }

    @Override
    public boolean existsByWebsiteIdAndUrl(WebsiteId websiteId, String url) {
        return crawledUrlRepository.existsByWebsiteIdAndUrl(websiteId.getId(), url);
    }

    @Override
    public List<CrawledUrl> findPendingUrls(WebsiteId websiteId, int maxDepth) {
        // 이 메서드는 나중에 필요시 구현 (현재는 사용되지 않음)
        return List.of();
    }

    @Override
    public long countByWebsiteId(WebsiteId websiteId) {
        return crawledUrlRepository.countByWebsiteId(websiteId.getId());
    }

    @Override
    public long countCrawledUrls(WebsiteId websiteId) {
        return crawledUrlRepository.countByWebsiteIdAndStatus(websiteId.getId(), CrawlStatus.CRAWLED);
    }

    @Override
    public long countFailedByWebsiteId(WebsiteId websiteId) {
        return crawledUrlRepository.countByWebsiteIdAndStatus(websiteId.getId(), CrawlStatus.FAILED);
    }
}