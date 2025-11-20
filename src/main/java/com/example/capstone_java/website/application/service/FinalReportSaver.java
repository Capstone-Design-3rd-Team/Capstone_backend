package com.example.capstone_java.website.application.service;

import com.example.capstone_java.website.adapter.in.dto.FinalReportDto;
import com.example.capstone_java.website.application.port.out.SaveFinalReportPort;
import com.example.capstone_java.website.application.port.out.SaveWebsitePort;
import com.example.capstone_java.website.domain.entity.Website;
import com.example.capstone_java.website.domain.vo.WebsiteId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 최종 보고서 저장 전용 컴포넌트
 *
 * 🔥 Self-Invocation 문제 해결:
 * - 같은 클래스 내에서 @Transactional 메서드를 호출하면 트랜잭션이 무시됨
 * - 별도 클래스로 분리하여 Spring 프록시가 정상 작동하도록 함
 * - REQUIRES_NEW가 확실하게 적용되어 즉시 커밋됨
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FinalReportSaver {

    private final SaveFinalReportPort saveFinalReportPort;
    private final SaveWebsitePort saveWebsitePort;

    /**
     * 최종 보고서와 Website를 새로운 트랜잭션으로 저장
     *
     * REQUIRES_NEW: 부모 트랜잭션과 독립적으로 즉시 커밋
     * → SSE 전송 전에 DB 커밋 완료 보장
     * → 외부 클래스이므로 프록시가 정상 작동
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveWithNewTransaction(WebsiteId websiteId, FinalReportDto finalReport, Website website) {
        log.info("🔄 [별도 트랜잭션] DB 저장 시작 - websiteId={}", websiteId.getId());

        // 1. 최종 보고서 DB 저장
        saveFinalReportPort.save(websiteId, finalReport);
        log.info("💾 최종 보고서 DB 저장 완료");

        // 2. Website 상태를 COMPLETE로 변경 및 저장
        Website completedWebsite = website.markCompleted();
        saveWebsitePort.save(completedWebsite);
        log.info("💾 Website 상태 COMPLETE로 변경 완료");

        // 메서드 종료 → 즉시 커밋! (REQUIRES_NEW)
        log.info("✅ [별도 트랜잭션] 커밋 완료 - 이제 데이터 조회 가능!");
    }
}
