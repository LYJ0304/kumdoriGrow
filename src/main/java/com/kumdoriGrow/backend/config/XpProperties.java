package com.kumdoriGrow.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "kumdori.xp")
public class XpProperties {
    private Map<String, BigDecimal> weights = new HashMap<>();

    // 기본 생성자에서 기본값 설정 (부팅 안정성 보장)
    public XpProperties() {
        weights = new HashMap<>();
        // 기본 가중치 설정
        weights.put("FRANCHISE", new BigDecimal("0.6"));
        weights.put("LOCAL", new BigDecimal("1.0"));
        weights.put("MARKET", new BigDecimal("2.0"));
    }

    public Map<String, BigDecimal> getWeights() { 
        return weights != null ? weights : new HashMap<>(); 
    }
    
    public void setWeights(Map<String, BigDecimal> weights) { 
        this.weights = weights != null ? weights : new HashMap<>(); 
    }

    public BigDecimal weightOf(String categoryCodeUpper) {
        Map<String, BigDecimal> safeWeights = getWeights();
        return safeWeights.getOrDefault(categoryCodeUpper, BigDecimal.ONE); // 기본 1.0배
    }
}
