package com.example.capstone_java.website.domain.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
@Getter
public final class WebsiteId {
    private Long id;

    public WebsiteId(final Long id) {
        this.id = id;
    }
}
