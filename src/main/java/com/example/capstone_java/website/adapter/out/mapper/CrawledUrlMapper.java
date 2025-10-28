package com.example.capstone_java.website.adapter.out.mapper;

import com.example.capstone_java.website.adapter.out.persistence.entity.CrawledUrlEntity;
import com.example.capstone_java.website.domain.entity.CrawledUrl;
import com.example.capstone_java.website.domain.vo.WebsiteId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface CrawledUrlMapper {

    @Mapping(target = "websiteId", source = "websiteId", qualifiedByName = "websiteIdToUuid")
    CrawledUrlEntity toEntity(CrawledUrl crawledUrl);

    @Mapping(target = "websiteId", source = "websiteId", qualifiedByName = "uuidToWebsiteId")
    CrawledUrl toDomain(CrawledUrlEntity entity);

    @Named("websiteIdToUuid")
    default UUID websiteIdToUuid(WebsiteId websiteId) {
        return websiteId != null ? websiteId.getId() : null;
    }

    @Named("uuidToWebsiteId")
    default WebsiteId uuidToWebsiteId(UUID uuid) {
        return uuid != null ? WebsiteId.of(uuid) : null;
    }
}