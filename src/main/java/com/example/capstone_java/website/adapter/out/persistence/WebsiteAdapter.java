package com.example.capstone_java.website.adapter.out.persistence;

import com.example.capstone_java.website.adapter.out.mapper.WebsiteMapper;
import com.example.capstone_java.website.adapter.out.persistence.entity.WebsiteEntity;
import com.example.capstone_java.website.adapter.out.persistence.repository.WebsiteJpaRepository;
import com.example.capstone_java.website.application.port.out.GetWebsitePort;
import com.example.capstone_java.website.application.port.out.SaveWebsitePort;
import com.example.capstone_java.website.domain.entity.Website;
import com.example.capstone_java.website.domain.vo.WebsiteId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class WebsiteAdapter implements GetWebsitePort,SaveWebsitePort {

    private final WebsiteMapper websiteMapper;
    private final WebsiteJpaRepository websiteJpaRepository;

    @Override
    public Website save(Website website) {
        WebsiteEntity websiteEntity = websiteMapper.toWebsiteEntity(website);
        websiteJpaRepository.save(websiteEntity);
        return websiteMapper.toWebsiteDomain(websiteEntity);
    }

    @Override
    public Optional<Website> findById(WebsiteId websiteId) {
        return websiteJpaRepository.findById(websiteId.getId())
                .map(websiteMapper::toWebsiteDomain);
    }
}
