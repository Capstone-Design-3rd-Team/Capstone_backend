package com.example.capstone_java.crawling.domain.vo;

import java.net.MalformedURLException;
import java.net.URL;


public record DomainUrl(String url) {
    /**
     * record의 Compact Constructor입니다.
     * 객체가 생성되는 시점에 URL 형식의 유효성을 검사
     */
    public DomainUrl{
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("유효하지 않은 URL 형식입니다: " + url, e);
        }
    }

    /**
     * @param other 비교할 다른 DomainUrl 객체
     * @return 동일 도메인 여부
     */
    public boolean isSameDomain(DomainUrl other) {
        try {
            // 각 URL의 호스트(도메인) 부분만 추출하여 비교
            return new URL(this.url).getHost().equalsIgnoreCase(new URL(other.url()).getHost());
        } catch (MalformedURLException e) {
            return false;
        }
    }
}
