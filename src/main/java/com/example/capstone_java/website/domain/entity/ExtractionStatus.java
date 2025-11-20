package com.example.capstone_java.website.domain.entity;

public enum ExtractionStatus {
    PENDING,    // 대기 중
    PROGRESS,   // 크롤링 진행 중
    ANALYZING,  // 크롤링 완료, AI 분석 진행 중
    COMPLETE,   // 모든 작업 완료 (크롤링 + AI 분석)
    FAILED      // 실패
}
