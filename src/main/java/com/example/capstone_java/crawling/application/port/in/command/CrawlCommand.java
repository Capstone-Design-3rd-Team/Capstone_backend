package com.example.capstone_java.crawling.application.port.in.command;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record CrawlCommand(
        @NotBlank(message = "URL은 비어 있을 수 없습니다.")
        @URL(message = "유효한 URL 형식이 아닙니다.")
        String url
) {
}
