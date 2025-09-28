package com.example.capstone_java.website.application.service;

import com.example.capstone_java.website.application.port.in.usecase.ExtractUrlsUseCase;
import com.example.capstone_java.website.application.port.out.SaveWebsitePort;
import com.example.capstone_java.website.domain.entity.Website;
import com.example.capstone_java.website.domain.event.ExtractionStartedEvent;
import com.example.capstone_java.website.domain.vo.WebsiteId;
import com.example.capstone_java.website.global.common.PublishEventPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommandWebsiteService implements ExtractUrlsUseCase {

    private final SaveWebsitePort saveWebsitePort;
    private final PublishEventPort publishEventPort;

    @Override
    @Transactional
    public WebsiteId execute(final String mainUrl) {
        Website website = Website.create(mainUrl).startExtraction();
        // 진행 상태로 저장
        Website savedWebsite = saveWebsitePort.save(website);

        ExtractionStartedEvent extractionStartedEvent = ExtractionStartedEvent.of(savedWebsite.getWebsiteId(), savedWebsite.getMainUrl());
        publishEventPort.publish(extractionStartedEvent);

        return savedWebsite.getWebsiteId();
    }
}
