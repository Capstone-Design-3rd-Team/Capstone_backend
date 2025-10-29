package com.example.capstone_java.website.adapter.out;

import com.example.capstone_java.website.application.port.out.JsoupPort;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class JsoupAdapter implements JsoupPort {

    private static final int TIMEOUT_MS = 10000; // 10 seconds
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    @Override
    public List<String> getCrawledUrls(String url) {
        List<String> validUrls = new ArrayList<>();

        try {
            log.info("크롤링 시작: {}", url);

            // 메인 URL의 도메인 추출
            String mainDomain = extractDomain(url);

            // Jsoup으로 HTML 문서 가져오기
            Document document = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .followRedirects(true)
                    .get();

            // 모든 링크 요소 추출 (a 태그의 href 속성)
            Elements linkElements = document.select("a[href]");

            log.info("발견된 링크 수: {}", linkElements.size());

            // 각 링크를 검증하고 유효한 URL만 추가
            for (Element link : linkElements) {
                String href = link.attr("abs:href"); // 절대 URL로 변환

                if (isValidUrl(href) && isSameDomain(href, mainDomain)) {
                    validUrls.add(href);
                }
            }

            log.info("유효한 URL 수 (같은 도메인만): {}", validUrls.size());

        } catch (IOException e) {
            log.error("URL 크롤링 실패: {}, 오류: {}", url, e.getMessage());
            throw new RuntimeException("웹페이지 크롤링 중 오류가 발생했습니다: " + url, e);
        }

        return validUrls;
    }

    /**
     * URL에서 도메인 추출
     */
    private String extractDomain(String url) {
        try {
            URL parsedUrl = new URL(url);
            return parsedUrl.getHost();
        } catch (MalformedURLException e) {
            return "";
        }
    }

    /**
     * 같은 도메인인지 확인
     */
    private boolean isSameDomain(String url, String targetDomain) {
        try {
            URL parsedUrl = new URL(url);
            return parsedUrl.getHost().equals(targetDomain);
        } catch (MalformedURLException e) {
            return false;
        }
    }

    /**
     * URL의 유효성을 검증합니다.
     * @param url 검증할 URL
     * @return 유효한 URL인지 여부
     */
    private boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        try {
            URL parsedUrl = new URL(url);
            String protocol = parsedUrl.getProtocol();
            String host = parsedUrl.getHost();

            // HTTP/HTTPS 프로토콜만 허용
            if (!protocol.equals("http") && !protocol.equals("https")) {
                return false;
            }

            // 호스트가 존재해야 함
            if (host == null || host.trim().isEmpty()) {
                return false;
            }

            // 앵커 링크(#) 필터링
            if (url.contains("#")) {
                return false;
            }

            // 불필요한 URL 패턴 필터링
            String urlLower = url.toLowerCase();
            if (urlLower.contains("javascript:") ||
                urlLower.contains("mailto:") ||
                urlLower.contains("tel:") ||
                urlLower.contains("ftp:") ||
                urlLower.endsWith(".pdf") ||
                urlLower.endsWith(".jpg") ||
                urlLower.endsWith(".png") ||
                urlLower.endsWith(".gif") ||
                urlLower.endsWith(".css") ||
                urlLower.endsWith(".js")) {
                return false;
            }

            return true;

        } catch (MalformedURLException e) {
            log.debug("잘못된 URL 형식: {}", url);
            return false;
        }
    }
}
