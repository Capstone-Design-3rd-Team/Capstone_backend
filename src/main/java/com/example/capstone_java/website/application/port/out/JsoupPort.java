package com.example.capstone_java.website.application.port.out;

import java.util.List;

public interface JsoupPort {
    List<String> getCrawledUrls(String url);
}
