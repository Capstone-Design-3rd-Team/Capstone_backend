package com.example.capstone_java.website.application.port.out;

import com.example.capstone_java.website.adapter.in.dto.FinalReportDto;
import com.example.capstone_java.website.domain.vo.WebsiteId;

/**
 * 최종 보고서 저장 Port
 */
public interface SaveFinalReportPort {

    /**
     * 최종 보고서를 DB에 저장
     *
     * @param websiteId 웹사이트 ID
     * @param finalReport 최종 보고서 DTO
     */
    void save(WebsiteId websiteId, FinalReportDto finalReport);
}
