package com.kumdoriGrow.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ocr.clova")
public record OcrProperties(
        String url,
        String secret
) {
    public OcrProperties {
        // 기본값 설정 - null-safe 처리
        if (url == null) {
            url = "";
        }
        if (secret == null) {
            secret = "";
        }
    }
}
