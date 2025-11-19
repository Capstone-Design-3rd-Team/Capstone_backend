package com.example.capstone_java.website.application.service;

import com.example.capstone_java.website.application.port.in.usecase.ExtractUrlsUseCase;
import com.example.capstone_java.website.application.port.out.GetWebsitePort;
import com.example.capstone_java.website.application.port.out.SaveWebsitePort;
import com.example.capstone_java.website.domain.entity.Website;
import com.example.capstone_java.website.domain.event.ExtractionStartedEvent;
import com.example.capstone_java.website.domain.exception.DuplicateAnalysisException;
import com.example.capstone_java.website.domain.vo.WebsiteId;
import com.example.capstone_java.website.application.event.EventDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommandWebsiteService implements ExtractUrlsUseCase {

    private final GetWebsitePort getWebsitePort;
    private final SaveWebsitePort saveWebsitePort;
    private final EventDispatcher eventDispatcher;

    @Override
    @Transactional
    public WebsiteId execute(final String clientId, final String mainUrl) {
        // 1. 사전 중복 체크 (사용자 친화적 메시지)
        getWebsitePort.findByClientIdAndMainUrl(clientId, mainUrl)
                .ifPresent(existing -> {
                    if (existing.isInProgress()) {
                        throw DuplicateAnalysisException.inProgress(clientId, mainUrl);
                    }
                    // 완료된 분석은 재요청 가능하므로 통과
                    log.info("완료된 분석 재요청: clientId={}, url={}", clientId, mainUrl);
                });

        // 2. Website 생성 및 저장
        Website website = Website.create(mainUrl, clientId);

        try {
            Website savedWebsite = saveWebsitePort.save(website);

            // 3. 추출 시작 이벤트 발행
            ExtractionStartedEvent extractionStartedEvent = ExtractionStartedEvent.of(
                    savedWebsite.getWebsiteId(),
                    savedWebsite.getMainUrl());

            eventDispatcher.dispatch(extractionStartedEvent);

            return savedWebsite.getWebsiteId();

        } catch (DataIntegrityViolationException e) {
            // 4. 유니크 제약 조건 위반 (동시 요청)
            log.error("중복 분석 요청 (동시성): clientId={}, url={}", clientId, mainUrl);
            throw new DuplicateAnalysisException("중복된 분석 요청입니다.");
        }
    }
}
