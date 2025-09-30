package com.example.capstone_java.website.adapter.out.persistence.repository;

import com.example.capstone_java.website.adapter.out.persistence.entity.WebsiteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebsiteJpaRepository extends JpaRepository<WebsiteEntity, Long> {
}
