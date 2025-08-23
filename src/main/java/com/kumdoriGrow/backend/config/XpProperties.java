package com.kumdoriGrow.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "kumdori.xp")
public class XpProperties {
    private Map<String, BigDecimal> weights = new HashMap<>();

    public Map<String, BigDecimal> getWeights() { return weights; }
    public void setWeights(Map<String, BigDecimal> weights) { this.weights = weights; }

    public BigDecimal weightOf(String categoryCodeUpper) {
        return weights.getOrDefault(categoryCodeUpper, BigDecimal.ONE); // 기본 1.0배
    }
}
