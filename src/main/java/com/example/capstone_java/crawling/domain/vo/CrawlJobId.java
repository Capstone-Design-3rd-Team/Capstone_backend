package com.example.capstone_java.crawling.domain.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

@Getter
@EqualsAndHashCode
public final class CrawlJobId {

    private final UUID uuid;

    private CrawlJobId(UUID uuid) {
        this.uuid = Objects.requireNonNull(uuid, "uuid must not be null");
    }
    // newId란 이름의 메서드로 생성하도록 하기
    public static CrawlJobId newId() {
        return new CrawlJobId(UUID.randomUUID());
    }
}
