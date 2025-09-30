package com.example.capstone_java.website.application.port.in.usecase;

import com.example.capstone_java.website.domain.vo.WebsiteId;

public interface CrawlUrlsUseCase {
    void startCrawling(WebsiteId websiteId, String mainUrl);
}
