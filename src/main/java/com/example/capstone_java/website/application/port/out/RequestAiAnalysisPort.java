package com.example.capstone_java.website.application.port.out;

/**
 * AI 서버에 접근성 분석을 요청하는 아웃바운드 포트
 *
 * 책임:
 * - Flask AI 서버에 분석 요청 전송
 * - 비동기 처리를 위한 콜백 URL 전달
 */
public interface RequestAiAnalysisPort {

    /**
     * AI 서버에 URL 분석을 요청합니다.
     *
     * @param websiteId 웹사이트 ID (콜백 시 매칭을 위해 필요)
     * @param url 분석할 웹페이지 URL
     * @param callbackUrl 분석 완료 후 결과를 전송받을 콜백 URL
     * @return 분석 요청의 작업 ID (task_id)
     */
    String requestAnalysis(String websiteId, String url, String callbackUrl);
}
