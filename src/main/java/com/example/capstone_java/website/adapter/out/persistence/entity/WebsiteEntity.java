package com.example.capstone_java.website.adapter.out.persistence.entity;

import com.example.capstone_java.website.domain.entity.ExtractionStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "website")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WebsiteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long websiteId;

    @Column(nullable = false, length = 2048)
    private String mainUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExtractionStatus extractionStatus;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}