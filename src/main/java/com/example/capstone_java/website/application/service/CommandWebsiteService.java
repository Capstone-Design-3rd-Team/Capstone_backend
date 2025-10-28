package com.example.capstone_java.website.application.service;

import com.example.capstone_java.website.application.port.in.usecase.ExtractUrlsUseCase;
import com.example.capstone_java.website.application.port.out.SaveWebsitePort;
import com.example.capstone_java.website.domain.entity.Website;
import com.example.capstone_java.website.domain.event.ExtractionStartedEvent;
import com.example.capstone_java.website.domain.vo.WebsiteId;
import com.example.capstone_java.website.application.event.EventDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommandWebsiteService implements ExtractUrlsUseCase {

    private final SaveWebsitePort saveWebsitePort;
    private final EventDispatcher eventDispatcher;

    @Override
    @Transactional
    public WebsiteId execute(final String mainUrl) {
        // PENDING 상태로 저장 (Consumer가 PROGRESS로 변경)
        Website website = Website.create(mainUrl);
        Website savedWebsite = saveWebsitePort.save(website);
        // websiteId,mainUrl,eventOccurredAt을 가지고 추출시작이벤트 생성
        ExtractionStartedEvent extractionStartedEvent = ExtractionStartedEvent.of(
                savedWebsite.getWebsiteId(),
                savedWebsite.getMainUrl());
        // 이벤트 produce 이후 ExtractionEventConsumer가 이벤트 소비
        eventDispatcher.dispatch(extractionStartedEvent);
        return savedWebsite.getWebsiteId();
    }
}
