package com.kumdoriGrow.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ocr")
public record OcrProperties(
        boolean enabled,
        String apiUrl,
        String apiKey
) {
    public OcrProperties {
        // 기본값 설정 - null-safe 처리
        if (apiUrl == null) {
            apiUrl = "";
        }
        if (apiKey == null) {
            apiKey = "";
        }
    }
}
