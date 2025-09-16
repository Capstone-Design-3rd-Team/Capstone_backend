package com.example.capstone_java.crawling.application.port.out;

import com.example.capstone_java.crawling.domain.CrawledUrl;

public interface SaveCrawledUrlPort {
    CrawledUrl save(CrawledUrl crawledUrl);
}
