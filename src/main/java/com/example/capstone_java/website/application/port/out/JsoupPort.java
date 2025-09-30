package com.example.capstone_java.website.application.port.out;

import java.util.Set;

public interface JsoupPort {
    Set<String> getCrawledUrls(String url);
}
