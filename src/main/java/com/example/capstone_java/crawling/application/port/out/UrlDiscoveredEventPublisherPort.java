package com.example.capstone_java.crawling.application.port.out;

import com.example.capstone_java.crawling.domain.event.UrlDiscoveredEvent;

public interface UrlDiscoveredEventPublisherPort {
    void publish(UrlDiscoveredEvent event);
}
