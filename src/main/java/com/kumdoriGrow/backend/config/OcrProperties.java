package com.kumdoriGrow.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ocr.clova")
public record OcrProperties(
        String url,
        String secret
) {}
