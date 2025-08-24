package com.kumdoriGrow.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ocr")
public record OcrProperties(
        boolean enabled,
        String apiUrl,
        String apiKey
) {
    public OcrProperties {
        // null-safe 기본값 설정 - 부팅 안정성 보장
        if (apiUrl == null) {
            apiUrl = "";
        }
        if (apiKey == null) {
            apiKey = "";
        }
    }
    
    // 기본값을 가진 생성자 추가 (바인딩 실패 시에도 부팅 가능)
    public OcrProperties() {
        this(false, "", "");
    }
}
