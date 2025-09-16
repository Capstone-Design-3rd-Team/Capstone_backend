package com.example.capstone_java.crawling.application.port.out;

import com.example.capstone_java.crawling.domain.CrawlJob;
import com.example.capstone_java.crawling.domain.vo.CrawlJobId;

public interface LoadCrawlJobPort {
    CrawlJob load(CrawlJobId crawlJobId);
}
