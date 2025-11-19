package com.example.capstone_java.website.application.port.out;

import com.example.capstone_java.website.domain.entity.AccessibilityReport;
import com.example.capstone_java.website.domain.vo.WebsiteId;

import java.util.List;
import java.util.Optional;

/**
 * 접근성 분석 보고서 저장 아웃바운드 포트
 *
 * 책임:
 * - 접근성 분석 보고서를 DB에 저장
 * - 기존 보고서 조회
 */
public interface SaveAccessibilityReportPort {

    /**
     * 접근성 분석 보고서를 저장합니다.
     *
     * @param report 저장할 분석 보고서
     * @return 저장된 보고서 (ID 포함)
     */
    AccessibilityReport save(AccessibilityReport report);

    /**
     * 특정 웹사이트의 모든 분석 보고서를 조회합니다.
     *
     * @param websiteId 웹사이트 ID
     * @return 분석 보고서 목록
     */
    List<AccessibilityReport> findByWebsiteId(WebsiteId websiteId);

    /**
     * 특정 URL의 분석 보고서를 조회합니다.
     *
     * @param url 조회할 URL
     * @return 분석 보고서 (Optional)
     */
    Optional<AccessibilityReport> findByUrl(String url);

    /**
     * 특정 웹사이트의 분석 보고서 개수를 조회합니다.
     *
     * @param websiteId 웹사이트 ID
     * @return 보고서 개수
     */
    long countByWebsiteId(WebsiteId websiteId);
}
