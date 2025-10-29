package com.example.capstone_java.website.adapter.out;

import com.example.capstone_java.website.application.port.out.CrawlStrategy;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Jsoup 기반 정적 HTML 크롤링 전략
 *
 * 책임: URL 추출만 담당 (검증/필터링은 Website 도메인이 함)
 */
@Slf4j
@Component
public class JsoupStrategy implements CrawlStrategy {

    private static final int TIMEOUT_MS = 10000;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    @Override
    public List<String> extractUrls(String url) {
        Set<String> uniqueUrls = new LinkedHashSet<>();  // 순서 유지하며 중복 제거
        String baseDomain = extractDomain(url);

        try {
            log.info("[Jsoup] 크롤링 시작: {}", url);

            Document document = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .followRedirects(true)
                    .get();

            Elements linkElements = document.select("a[href]");

            for (Element link : linkElements) {
                String href = link.attr("abs:href");

                // URL 정규화 및 필터링
                String normalizedUrl = normalizeUrl(href, baseDomain);
                if (normalizedUrl != null) {
                    uniqueUrls.add(normalizedUrl);
                }
            }

            log.info("[Jsoup] 추출된 URL 수: {} (중복 제거 후)", uniqueUrls.size());

        } catch (IOException e) {
            log.warn("[Jsoup] 크롤링 실패: {} - {}", url, e.getMessage());
            // 예외를 던지지 않고 빈 리스트 반환 (Playwright fallback을 위해)
            return new ArrayList<>();
        }

        return new ArrayList<>(uniqueUrls);
    }

    /**
     * URL 정규화 및 필터링
     *
     * @param url 원본 URL
     * @param baseDomain 기준 도메인
     * @return 정규화된 URL (필터링되면 null)
     */
    private String normalizeUrl(String url, String baseDomain) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        // javascript: 프로토콜 제거
        if (url.startsWith("javascript:") || url.startsWith("mailto:") || url.startsWith("tel:")) {
            return null;
        }

        try {
            URI uri = new URI(url);

            // Fragment(#) 제거
            String normalizedUrl = new URI(
                uri.getScheme(),
                uri.getAuthority(),
                uri.getPath(),
                uri.getQuery(),
                null  // fragment 제거
            ).toString();

            // 같은 도메인만 허용
            String urlDomain = extractDomain(normalizedUrl);
            if (!baseDomain.equals(urlDomain)) {
                log.debug("[Jsoup] 외부 도메인 제외: {}", normalizedUrl);
                return null;
            }

            return normalizedUrl;

        } catch (URISyntaxException e) {
            log.debug("[Jsoup] 잘못된 URL 형식: {}", url);
            return null;
        }
    }

    /**
     * URL에서 도메인 추출
     */
    private String extractDomain(String url) {
        try {
            URI uri = new URI(url);
            return uri.getHost();
        } catch (URISyntaxException e) {
            return "";
        }
    }

    @Override
    public boolean canHandle(String url) {
        return true;  // 기본 전략
    }

    @Override
    public int getPriority() {
        return 1;  // 먼저 시도
    }

    @Override
    public String getName() {
        return "Jsoup";
    }
}
