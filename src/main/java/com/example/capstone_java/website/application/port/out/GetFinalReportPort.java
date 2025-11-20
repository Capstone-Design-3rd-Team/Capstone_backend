package com.example.capstone_java.website.application.port.out;

import com.example.capstone_java.website.adapter.in.dto.FinalReportDto;
import com.example.capstone_java.website.domain.vo.WebsiteId;

import java.util.Optional;

/**
 * 최종 보고서 조회 Port
 */
public interface GetFinalReportPort {

    /**
     * WebsiteId로 최종 보고서 조회
     *
     * @param websiteId 웹사이트 ID
     * @return 최종 보고서 (없으면 Optional.empty())
     */
    Optional<FinalReportDto> findByWebsiteId(WebsiteId websiteId);
}
