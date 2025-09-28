package com.example.capstone_java.website.application.service;

import com.example.capstone_java.website.application.port.in.usecase.CrawlUrlsUseCase;
import com.example.capstone_java.website.application.port.out.JsoupPort;
import com.example.capstone_java.website.domain.vo.WebsiteId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlCrawlingService implements CrawlUrlsUseCase {

    private final JsoupPort jsoupPort;

    private final ValidateUrl validateUrl; // 이것도 ddd에서 자신의 url을 자기가 검증 가능함
    private final ExtractUrl extractUrl; // 이건 생각해보면 adapter임 jsoup을 사용하는

    @Override
    public void startCrawling(WebsiteId websiteId, String mainUrl) {
        //websiteID로 website 도메인 찾기

        //
    }
}
