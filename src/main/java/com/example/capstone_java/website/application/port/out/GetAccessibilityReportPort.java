package com.example.capstone_java.website.application.port.out;

import com.example.capstone_java.website.domain.entity.AccessibilityReport;
import com.example.capstone_java.website.domain.vo.WebsiteId;

import java.util.List;

/**
 * AccessibilityReport 조회 Port
 */
public interface GetAccessibilityReportPort {

    /**
     * WebsiteId로 분석 완료된 개수 조회
     */
    long countByWebsiteId(WebsiteId websiteId);

    /**
     * WebsiteId로 모든 분석 결과 조회
     */
    List<AccessibilityReport> findAllByWebsiteId(WebsiteId websiteId);
}
