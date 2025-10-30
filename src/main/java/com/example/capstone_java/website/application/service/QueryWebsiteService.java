package com.example.capstone_java.website.application.service;

import com.example.capstone_java.website.application.port.in.responseDto.WebsiteResponse;
import com.example.capstone_java.website.application.port.in.usecase.GetWebsiteUseCase;
import com.example.capstone_java.website.application.port.out.GetWebsitePort;
import com.example.capstone_java.website.domain.vo.WebsiteId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QueryWebsiteService implements GetWebsiteUseCase {

    private final GetWebsitePort getWebsitePort;

    @Override
    public WebsiteResponse getWebsite(WebsiteId websiteId) {
       return getWebsitePort.findById(websiteId)
               .map(website -> new WebsiteResponse(
                       website.getWebsiteId(),
                       website.getMainUrl(),
                       website.getExtractionStatus(),
                       website.getCreatedAt()
               ))
               .orElseThrow(() -> new IllegalArgumentException("Website id not found"));

    }
}
