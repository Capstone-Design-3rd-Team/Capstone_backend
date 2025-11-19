package com.example.capstone_java.website.adapter.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.URL;

@Schema(description = "크롤링 시작 요청 정보")
public record CrawlStartRequest(
        @Schema(
                description = "웹사이트 등록 시 발급받은 클라이언트 ID",
                example = "client_user_12345",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "클라이언트 ID는 필수입니다.") // 빈 문자열, null, 공백만 있는 경우 차단
        String clientId,

        @Schema(
                description = "분석할 웹사이트의 메인 URL (프로토콜 포함)",
                example = "https://www.naver.com",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "분석할 URL은 필수입니다.")
        @Pattern(
                regexp = "^(http|https)://.*",
                message = "URL은 http:// 또는 https://로 시작해야 합니다."
        )
        String mainUrl
) {
    //::todo:: 생성 시에 mainUrl에 아무값도 없으면 예외
}
