package com.example.capstone_java.crawling.domain.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Objects;

@Getter
@EqualsAndHashCode
public final class CrawledUrlId {
    private Long id;

    private CrawledUrlId(Long id){
        this.id = Objects.requireNonNull(id, "id cannot be null");
    }

    public static CrawledUrlId newId(Long id){
        return new CrawledUrlId(id);
    }
}
