package com.example.capstone_java.website.domain.vo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.UUID;

@EqualsAndHashCode
@Getter
public final class WebsiteId {
    private final UUID id;

    @JsonCreator
    public WebsiteId(@JsonProperty("id") final UUID id) {
        this.id = id;
    }

    public static WebsiteId of(UUID id) {
        return new WebsiteId(id);
    }

    public static WebsiteId generate() {
        return new WebsiteId(UUID.randomUUID());
    }
}
