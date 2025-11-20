package com.example.capstone_java.website.adapter.out.persistence.repository;

import com.example.capstone_java.website.adapter.out.persistence.entity.FinalReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 최종 보고서 Repository
 */
@Repository
public interface FinalReportJpaRepository extends JpaRepository<FinalReportEntity, Long> {

    /**
     * WebsiteId로 최종 보고서 조회
     */
    Optional<FinalReportEntity> findByWebsiteId(UUID websiteId);

    /**
     * WebsiteId로 존재 여부 확인
     */
    boolean existsByWebsiteId(UUID websiteId);
}
