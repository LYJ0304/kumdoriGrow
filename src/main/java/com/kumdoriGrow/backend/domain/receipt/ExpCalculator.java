package com.kumdoriGrow.backend.domain.receipt;

import com.kumdoriGrow.backend.config.XpProperties;
import com.kumdoriGrow.backend.domain.category.Category;
import com.kumdoriGrow.backend.domain.category.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ExpCalculator {
    private final XpProperties xpProperties;
    private final CategoryRepository categoryRepository;

    // 설정이 없더라도 동작하도록 디폴트 맵(하드코딩) 준비
    private static final Map<String, BigDecimal> DEFAULT_WEIGHTS = Map.of(
            "FRANCHISE", new BigDecimal("1.0"),
            "LOCAL",     new BigDecimal("1.5"),
            "MARKET",    new BigDecimal("2.0")  // 전통시장&특산물
    );

    /**
     * 카테고리별 경험치 계산
     * 우선순위: 1) DB Category 테이블 2) application.yml 3) DEFAULT_WEIGHTS
     */
    public int calcExp(long amountWon, String categoryCode) {
        String key = categoryCode == null ? "" : categoryCode.trim().toUpperCase();
        
        BigDecimal weight = getWeight(key);
        
        // 금액 × weight의 간소화된 계산 (경험치 = floor(금액 × weight / 100))
        BigDecimal exp = new BigDecimal(amountWon).multiply(weight).divide(new BigDecimal("100"), 0, RoundingMode.FLOOR);
        return Math.max(1, exp.intValue()); // 최소 1 경험치
    }
    
    private BigDecimal getWeight(String categoryCode) {
        // 1) DB에서 카테고리 조회
        Optional<Category> category = categoryRepository.findById(categoryCode);
        if (category.isPresent()) {
            return BigDecimal.valueOf(category.get().getWeight());
        }
        
        // 2) application.yml 설정 확인
        if (xpProperties != null && xpProperties.getWeights() != null) {
            BigDecimal configWeight = xpProperties.getWeights().get(categoryCode);
            if (configWeight != null) {
                return configWeight;
            }
        }
        
        // 3) 기본값 사용
        return DEFAULT_WEIGHTS.getOrDefault(categoryCode, BigDecimal.ONE);
    }

    public int levelOf(long totalExp) {
        if (totalExp < 100) return 1;
        if (totalExp < 500) return 2;
        if (totalExp < 1000) return 3;
        if (totalExp < 2000) return 4;
        if (totalExp < 5000) return 5;
        
        // 5000 이상은 1000당 1레벨씩, 최대 30레벨
        int calculatedLevel = 5 + (int) ((totalExp - 5000) / 1000);
        return Math.min(calculatedLevel, 30);
    }
}
