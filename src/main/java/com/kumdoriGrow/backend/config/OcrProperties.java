package com.kumdoriGrow.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ocr.clova")
public record OcrProperties(
        String url,
        String secret
) {}
