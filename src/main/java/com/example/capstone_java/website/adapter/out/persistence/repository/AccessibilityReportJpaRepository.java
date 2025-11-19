package com.example.capstone_java.website.adapter.out.persistence.repository;

import com.example.capstone_java.website.adapter.out.persistence.entity.AccessibilityReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 접근성 분석 보고서 JPA 리포지토리
 */
@Repository
public interface AccessibilityReportJpaRepository extends JpaRepository<AccessibilityReportEntity, Long> {

    /**
     * 특정 웹사이트의 모든 분석 보고서 조회
     */
    List<AccessibilityReportEntity> findByWebsiteIdOrderByAnalyzedAtDesc(UUID websiteId);

    /**
     * 특정 URL의 분석 보고서 조회
     */
    Optional<AccessibilityReportEntity> findByUrl(String url);

    /**
     * 특정 웹사이트와 URL의 분석 보고서 조회
     */
    Optional<AccessibilityReportEntity> findByWebsiteIdAndUrl(UUID websiteId, String url);

    /**
     * Task ID로 분석 보고서 조회
     */
    Optional<AccessibilityReportEntity> findByTaskId(String taskId);

    /**
     * 특정 웹사이트의 분석 보고서 개수 조회
     */
    long countByWebsiteId(UUID websiteId);

    /**
     * 특정 URL의 분석 보고서가 이미 존재하는지 확인
     */
    boolean existsByUrl(String url);
}
