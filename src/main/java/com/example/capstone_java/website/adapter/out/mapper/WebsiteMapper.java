package com.example.capstone_java.website.adapter.out.mapper;

import com.example.capstone_java.website.adapter.out.persistence.entity.WebsiteEntity;
import com.example.capstone_java.website.domain.entity.Website;
import com.example.capstone_java.website.domain.vo.WebsiteId;
import com.example.capstone_java.website.global.config.CrawlConfiguration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface WebsiteMapper {

    @Mapping(target = "websiteId", source = "websiteId.id")
    @Mapping(target = "maxDepth", source = "crawlConfig.maxDepth")
    @Mapping(target = "maxTotalUrls", source = "crawlConfig.maxTotalUrls")
    @Mapping(target = "maxUrlsPerPage", source = "crawlConfig.maxUrlsPerPage")
    @Mapping(target = "maxDurationMinutes", source = "crawlConfig.maxDuration", qualifiedByName = "durationToMinutes")
    @Mapping(target = "allowedPaths", source = "crawlConfig.allowedPaths", qualifiedByName = "setToJson")
    @Mapping(target = "excludedPaths", source = "crawlConfig.excludedPaths", qualifiedByName = "setToJson")
    WebsiteEntity toWebsiteEntity(Website website);

    @Mapping(target = "websiteId", source = "websiteId", qualifiedByName = "idToWebsiteId")
    @Mapping(target = "crawlConfig", source = ".", qualifiedByName = "entityToCrawlConfig")
    Website toWebsiteDomain(WebsiteEntity websiteEntity);

    @Named("idToWebsiteId")
    default WebsiteId idToWebsiteId(UUID id) {
        return id != null ? new WebsiteId(id) : null;
    }

    @Named("durationToMinutes")
    default Long durationToMinutes(Duration duration) {
        return duration != null ? duration.toMinutes() : null;
    }

    @Named("setToJson")
    default String setToJson(Set<String> set) {
        if (set == null || set.isEmpty()) {
            return null;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(set);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Named("entityToCrawlConfig")
    default CrawlConfiguration entityToCrawlConfig(WebsiteEntity entity) {
        ObjectMapper mapper = new ObjectMapper();

        Set<String> allowedPaths = parseJsonToSet(entity.getAllowedPaths(), mapper);
        Set<String> excludedPaths = parseJsonToSet(entity.getExcludedPaths(), mapper);

        return new CrawlConfiguration(
            entity.getMaxDepth(),
            entity.getMaxTotalUrls(),
            entity.getMaxUrlsPerPage(),
            Duration.ofMinutes(entity.getMaxDurationMinutes()),
            allowedPaths,
            excludedPaths
        );
    }

    default Set<String> parseJsonToSet(String json, ObjectMapper mapper) {
        if (json == null || json.trim().isEmpty()) {
            return Set.of();
        }
        try {
            return mapper.readValue(json, new TypeReference<Set<String>>() {});
        } catch (JsonProcessingException e) {
            return Set.of();
        }
    }
}
