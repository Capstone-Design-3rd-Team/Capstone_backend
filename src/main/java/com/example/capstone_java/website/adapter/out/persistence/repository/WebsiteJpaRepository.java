package com.example.capstone_java.website.adapter.out.persistence.repository;

import com.example.capstone_java.website.adapter.out.persistence.entity.WebsiteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WebsiteJpaRepository extends JpaRepository<WebsiteEntity, UUID> {

    /**
     * clientId와 mainUrl로 Website 조회
     */
    Optional<WebsiteEntity> findByClientIdAndMainUrl(String clientId, String mainUrl);
}
