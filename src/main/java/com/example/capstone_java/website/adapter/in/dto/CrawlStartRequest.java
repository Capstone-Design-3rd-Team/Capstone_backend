package com.example.capstone_java.website.adapter.in.dto;

import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;

public record CrawlStartRequest(
        @URL
        @NotNull(message = "url은 비어 있으면 안됩니다")
        String mainUrl
) {
    //::todo:: 생성 시에 mainUrl에 아무값도 없으면 예외
}
