package com.example.capstone_java.website.adapter.out;

import com.example.capstone_java.website.adapter.in.dto.FinalReportDto;
import com.example.capstone_java.website.adapter.out.persistence.entity.FinalReportEntity;
import com.example.capstone_java.website.adapter.out.persistence.repository.FinalReportJpaRepository;
import com.example.capstone_java.website.application.port.out.GetFinalReportPort;
import com.example.capstone_java.website.application.port.out.SaveFinalReportPort;
import com.example.capstone_java.website.domain.vo.WebsiteId;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * 최종 보고서 Adapter
 *
 * 책임: FinalReportDto와 FinalReportEntity 간 변환 및 DB 저장/조회
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FinalReportAdapter implements SaveFinalReportPort, GetFinalReportPort {

    private final FinalReportJpaRepository finalReportRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void save(WebsiteId websiteId, FinalReportDto finalReport) {
        // DTO를 Map으로 변환 (JSON 저장용)
        @SuppressWarnings("unchecked")
        Map<String, Object> reportJson = objectMapper.convertValue(finalReport, Map.class);

        FinalReportEntity entity = FinalReportEntity.create(
                websiteId.getId(),
                reportJson,
                finalReport.getAverageScore(),
                finalReport.getUrlReports() != null ? finalReport.getUrlReports().size() : 0
        );

        finalReportRepository.save(entity);
        log.info("최종 보고서 DB 저장 완료: websiteId={}, score={}, urls={}",
                websiteId.getId(), finalReport.getAverageScore(),
                finalReport.getUrlReports() != null ? finalReport.getUrlReports().size() : 0);
    }

    @Override
    public Optional<FinalReportDto> findByWebsiteId(WebsiteId websiteId) {
        return finalReportRepository.findByWebsiteId(websiteId.getId())
                .map(entity -> objectMapper.convertValue(entity.getReportJson(), FinalReportDto.class));
    }
}
