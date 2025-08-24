package com.kumdoriGrow.backend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConditionalOnProperty(prefix = "ocr", name = "enabled", havingValue = "true", matchIfMissing = false)
public class RestClientConfig {
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(5000);
        f.setReadTimeout(15000);
        return new RestTemplate(f);
    }
}
