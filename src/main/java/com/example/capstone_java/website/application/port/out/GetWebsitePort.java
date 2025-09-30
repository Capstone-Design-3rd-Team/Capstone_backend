package com.example.capstone_java.website.application.port.out;

import com.example.capstone_java.website.domain.entity.Website;
import com.example.capstone_java.website.domain.vo.WebsiteId;

import java.util.Optional;

public interface GetWebsitePort {
    Optional<Website> findById(WebsiteId websiteId);
}
