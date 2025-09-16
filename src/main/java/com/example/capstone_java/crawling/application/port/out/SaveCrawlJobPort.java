package com.example.capstone_java.crawling.application.port.out;

import com.example.capstone_java.crawling.domain.CrawlJob;

public interface SaveCrawlJobPort {
    CrawlJob save(CrawlJob crawlJob);
}
