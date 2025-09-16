package com.example.capstone_java.crawling.application.port.out;

import com.example.capstone_java.crawling.domain.event.UrlFetchEvent;

public interface UrlFetchEventPublisherPort {
    void publish(UrlFetchEvent crawledUrl);
}
