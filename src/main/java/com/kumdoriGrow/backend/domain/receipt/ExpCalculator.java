package com.kumdoriGrow.backend.domain.receipt;

import com.kumdoriGrow.backend.config.XpProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Component
public class ExpCalculator {
    private final XpProperties xpProperties;

    // 설정이 없더라도 동작하도록 디폴트 맵(하드코딩) 준비
    private static final Map<String, BigDecimal> DEFAULT_WEIGHTS = Map.of(
            "FRANCHISE", new BigDecimal("0.6"),
            "LOCAL",     new BigDecimal("1.0"),
            "MARKET",    new BigDecimal("2.0")  // 전통시장&특산물
    );

    public ExpCalculator(XpProperties xpProperties) {
        this.xpProperties = xpProperties;
    }

    public int calcExp(long amountWon, String categoryCode) {
        String key = categoryCode == null ? "" : categoryCode.trim().toUpperCase();

        // 1) application.yml에 있으면 그 값 사용, 없으면 DEFAULT 사용
        BigDecimal w = xpProperties != null && xpProperties.getWeights() != null
                ? xpProperties.getWeights().getOrDefault(key, DEFAULT_WEIGHTS.getOrDefault(key, BigDecimal.ONE))
                : DEFAULT_WEIGHTS.getOrDefault(key, BigDecimal.ONE);

        // 금액(정수) × 배수(실수) → 반올림하여 정수 경험치
        BigDecimal exp = new BigDecimal(amountWon).multiply(w);
        return exp.setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    public int levelOf(long totalExp) {
        return (int)(totalExp / 100) + 1;
    }
}
