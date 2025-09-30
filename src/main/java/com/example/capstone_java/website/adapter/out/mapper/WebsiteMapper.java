package com.example.capstone_java.website.adapter.out.mapper;

import com.example.capstone_java.website.adapter.out.persistence.entity.WebsiteEntity;
import com.example.capstone_java.website.domain.entity.Website;
import com.example.capstone_java.website.domain.vo.WebsiteId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface WebsiteMapper {

    @Mapping(target = "websiteId", source = "websiteId.id")
    WebsiteEntity toWebsiteEntity(Website website);

    @Mapping(target = "websiteId", source = "websiteId", qualifiedByName = "idToWebsiteId")
    Website toWebsiteDomain(WebsiteEntity websiteEntity);

    @Named("idToWebsiteId")
    default WebsiteId idToWebsiteId(Long id) {
        return id != null ? new WebsiteId(id) : null;
    }
}
