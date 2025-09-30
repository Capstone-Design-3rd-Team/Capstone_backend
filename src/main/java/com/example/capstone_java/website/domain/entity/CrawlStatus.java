package com.example.capstone_java.website.domain.entity;

public enum CrawlStatus {
    DISCOVERED,  // URL이 발견됨
    CRAWLED,     // 크롤링 완료
    FAILED       // 크롤링 실패
}