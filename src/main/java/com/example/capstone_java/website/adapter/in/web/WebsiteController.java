package com.example.capstone_java.website.adapter.in.web;

import com.example.capstone_java.website.adapter.in.dto.CrawlStartRequest;
import com.example.capstone_java.website.adapter.in.dto.CrawlStartResponse;
import com.example.capstone_java.website.adapter.in.dto.CrawledUrlResponse;
import com.example.capstone_java.website.adapter.in.dto.WebsiteStatusResponse;
import com.example.capstone_java.website.application.port.in.usecase.ExtractUrlsUseCase;
import com.example.capstone_java.website.application.port.out.GetWebsitePort;
import com.example.capstone_java.website.application.port.out.SaveCrawledUrlPort;
import com.example.capstone_java.website.domain.entity.CrawledUrl;
import com.example.capstone_java.website.domain.entity.Website;
import com.example.capstone_java.website.domain.vo.WebsiteId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/websites")
@RequiredArgsConstructor
public class WebsiteController {

    private final ExtractUrlsUseCase extractUrlsUseCase;
    private final GetWebsitePort getWebsitePort;
    private final SaveCrawledUrlPort saveCrawledUrlPort;

    /**
     * 웹사이트 크롤링 시작
     */
    @PostMapping("/crawl")
    public ResponseEntity<CrawlStartResponse> startCrawling(@RequestBody @Valid CrawlStartRequest request)
    {
        try {
            log.info("크롤링 시작 요청: URL={}", request.mainUrl());

            WebsiteId websiteId = extractUrlsUseCase.execute(request.mainUrl());

            return ResponseEntity.ok(new CrawlStartResponse(
                websiteId.getId(),
                request.mainUrl(),
                "크롤링이 시작되었습니다."
            ));

        } catch (Exception e) {
            log.error("크롤링 시작 실패: URL={}, Error={}", request.mainUrl(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new CrawlStartResponse(null, request.mainUrl(), "크롤링 시작 실패: " + e.getMessage()));
        }
    }

    /**
     * 웹사이트 상태 조회
     */
    @GetMapping("/{websiteId}")
    public ResponseEntity<WebsiteStatusResponse> getWebsiteStatus(@PathVariable UUID websiteId) {
        try {
            WebsiteId id = new WebsiteId(websiteId);
            Website website = getWebsitePort.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Website not found: " + websiteId));

            return ResponseEntity.ok(new WebsiteStatusResponse(
                website.getWebsiteId().getId(),
                website.getMainUrl(),
                website.getExtractionStatus().name(),
                website.getCrawlConfig().maxDepth(),
                website.getCrawlConfig().maxTotalUrls(),
                website.getCreatedAt()
            ));

        } catch (Exception e) {
            log.error("웹사이트 상태 조회 실패: websiteId={}, Error={}", websiteId, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 크롤링된 URL 목록 조회
     */
    @GetMapping("/{websiteId}/crawled-urls")
    public ResponseEntity<List<CrawledUrlResponse>> getCrawledUrls(@PathVariable UUID websiteId) {
        try {
            WebsiteId id = new WebsiteId(websiteId);
            List<CrawledUrl> crawledUrls = saveCrawledUrlPort.findByWebsiteId(id);

            List<CrawledUrlResponse> response = crawledUrls.stream()
                    .map(url -> new CrawledUrlResponse(
                        url.getUrl(),
                        url.getParentUrl(),
                        url.getDepth(),
                        url.getStatus().name(),
                        url.getDiscoveredAt(),
                        url.getCrawledAt()
                    ))
                    .toList();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("크롤링된 URL 조회 실패: websiteId={}, Error={}", websiteId, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }
}