package com.example.capstone_java.website.application.port.out;

import com.example.capstone_java.website.domain.entity.Website;
import com.example.capstone_java.website.domain.vo.WebsiteId;

import java.util.Optional;

public interface GetWebsitePort {
    Optional<Website> findById(WebsiteId websiteId);

    /**
     * clientId와 mainUrl로 Website 조회 (중복 체크용)
     */
    Optional<Website> findByClientIdAndMainUrl(String clientId, String mainUrl);
}
