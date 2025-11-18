package com.example.capstone_java.website.adapter.out;

import com.example.capstone_java.website.adapter.out.mapper.AccessibilityReportMapper;
import com.example.capstone_java.website.adapter.out.persistence.entity.AccessibilityReportEntity;
import com.example.capstone_java.website.adapter.out.persistence.repository.AccessibilityReportJpaRepository;
import com.example.capstone_java.website.application.port.out.SaveAccessibilityReportPort;
import com.example.capstone_java.website.domain.entity.AccessibilityReport;
import com.example.capstone_java.website.domain.vo.WebsiteId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 접근성 분석 보고서 저장 어댑터
 *
 * 도메인 객체(AccessibilityReport)를 JPA 엔티티로 변환하여 저장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessibilityReportAdapter implements SaveAccessibilityReportPort {

    private final AccessibilityReportJpaRepository repository;
    private final AccessibilityReportMapper mapper;

    @Override
    public AccessibilityReport save(AccessibilityReport report) {
        log.info("접근성 분석 보고서 저장 시작 - URL: {}", report.getUrl());

        // 도메인 객체를 JPA 엔티티로 변환
        AccessibilityReportEntity entity = mapper.toEntityFromDomain(report);

        // DB에 저장
        AccessibilityReportEntity savedEntity = repository.save(entity);

        log.info("접근성 분석 보고서 저장 완료 - ID: {}, URL: {}", savedEntity.getId(), savedEntity.getUrl());

        // 저장된 엔티티를 도메인 객체로 다시 변환하여 반환
        return mapper.toDomainWithId(savedEntity);
    }

    @Override
    public List<AccessibilityReport> findByWebsiteId(WebsiteId websiteId) {
        log.debug("웹사이트 분석 보고서 조회 - WebsiteId: {}", websiteId.getId());

        List<AccessibilityReportEntity> entities = repository.findByWebsiteIdOrderByAnalyzedAtDesc(websiteId.getId());

        return entities.stream()
                .map(mapper::toDomainWithId)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<AccessibilityReport> findByUrl(String url) {
        log.debug("URL 분석 보고서 조회 - URL: {}", url);

        return repository.findByUrl(url)
                .map(mapper::toDomainWithId);
    }

    @Override
    public long countByWebsiteId(WebsiteId websiteId) {
        return repository.countByWebsiteId(websiteId.getId());
    }
}
