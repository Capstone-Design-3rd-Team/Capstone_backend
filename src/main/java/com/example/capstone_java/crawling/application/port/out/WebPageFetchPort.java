package com.example.capstone_java.crawling.application.port.out;

import com.example.capstone_java.crawling.domain.vo.DomainUrl;

import java.util.Optional;
import java.util.Set;

public interface WebPageFetchPort {
    Optional<Set<DomainUrl>> fetchUrls(DomainUrl url);
}
