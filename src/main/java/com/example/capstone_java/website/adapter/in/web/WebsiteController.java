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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Website Crawling", description = "웹사이트 등록 및 크롤링 관리 API")
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
    @Operation(
            summary = "크롤링 시작 요청",
            description = "분석할 웹사이트 URL을 등록하고 크롤링(URL 추출) 작업을 시작합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "크롤링 시작 성공",
                    content = @Content(schema = @Schema(implementation = CrawlStartResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 URL 형식이거나 요청 실패")
    })
    @PostMapping("/crawl")
    public ResponseEntity<CrawlStartResponse> startCrawling(@RequestBody @Valid CrawlStartRequest request)
    {
        try {
            log.info("크롤링 시작 요청: clientId={}, URL={}", request.clientId(), request.mainUrl());

            WebsiteId websiteId = extractUrlsUseCase.execute(request.clientId(), request.mainUrl());

            return ResponseEntity.ok(new CrawlStartResponse(
                websiteId.getId(),
                request.mainUrl(),
                "크롤링이 시작되었습니다."
            ));

        } catch (Exception e) {
            log.error("크롤링 시작 실패: clientId={}, URL={}, Error={}", request.clientId(), request.mainUrl(), e.getMessage(), e);
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