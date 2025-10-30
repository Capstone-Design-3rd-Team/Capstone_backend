package com.example.capstone_java.website.adapter.out.persistence.repository;

import com.example.capstone_java.website.adapter.out.persistence.entity.WebsiteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WebsiteJpaRepository extends JpaRepository<WebsiteEntity, UUID> {
}
