package com.example.capstone_java.crawling.application.port.in.usecase;

import com.example.capstone_java.crawling.application.port.in.command.CrawlCommand;

import java.util.UUID;

public interface CrawlUseCase {
    UUID startCrawling(CrawlCommand crawlCommand);
}
