package com.example.capstone_java.website.application.port.in.usecase;

import com.example.capstone_java.website.application.port.in.responseDto.WebsiteResponse;
import com.example.capstone_java.website.domain.vo.WebsiteId;

public interface GetWebsiteUseCase {
    WebsiteResponse getWebsite(WebsiteId websiteId);
}
