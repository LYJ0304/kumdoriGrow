package com.kumdoriGrow.backend.domain.receipt;

import com.kumdoriGrow.backend.config.XpProperties;
import com.kumdoriGrow.backend.domain.category.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ExpCalculatorTest {
    
    @Mock
    private XpProperties xpProperties;
    
    @Mock
    private CategoryRepository categoryRepository;
    
    private ExpCalculator expCalculator;
    
    @BeforeEach
    void setUp() {
        expCalculator = new ExpCalculator(xpProperties, categoryRepository);
    }
    
    @Test
    void levelOf_shouldReturnCorrectLevels_forInitialLevels() {
        // 레벨 1-5 테스트
        assertThat(expCalculator.levelOf(0)).isEqualTo(1);
        assertThat(expCalculator.levelOf(50)).isEqualTo(1);
        assertThat(expCalculator.levelOf(99)).isEqualTo(1);
        
        assertThat(expCalculator.levelOf(100)).isEqualTo(2);
        assertThat(expCalculator.levelOf(499)).isEqualTo(2);
        
        assertThat(expCalculator.levelOf(500)).isEqualTo(3);
        assertThat(expCalculator.levelOf(999)).isEqualTo(3);
        
        assertThat(expCalculator.levelOf(1000)).isEqualTo(4);
        assertThat(expCalculator.levelOf(1999)).isEqualTo(4);
        
        assertThat(expCalculator.levelOf(2000)).isEqualTo(5);
        assertThat(expCalculator.levelOf(4999)).isEqualTo(5);
    }
    
    @Test
    void levelOf_shouldReturnCorrectLevels_forProgressiveLevels() {
        // 레벨 6-29 테스트 (1000당 1레벨)
        assertThat(expCalculator.levelOf(5000)).isEqualTo(5); // 5레벨 경계
        assertThat(expCalculator.levelOf(6000)).isEqualTo(6); // 6레벨
        assertThat(expCalculator.levelOf(7000)).isEqualTo(7); // 7레벨
        assertThat(expCalculator.levelOf(15000)).isEqualTo(15); // 15레벨
        assertThat(expCalculator.levelOf(25000)).isEqualTo(25); // 25레벨
        assertThat(expCalculator.levelOf(29000)).isEqualTo(29); // 29레벨
    }
    
    @Test
    void levelOf_shouldCapAt30_forHighExperience() {
        // 30레벨 도달
        assertThat(expCalculator.levelOf(30000)).isEqualTo(30); // 정확히 30레벨
        
        // 30레벨 초과해도 30레벨로 제한
        assertThat(expCalculator.levelOf(35000)).isEqualTo(30);
        assertThat(expCalculator.levelOf(50000)).isEqualTo(30);
        assertThat(expCalculator.levelOf(100000)).isEqualTo(30);
        assertThat(expCalculator.levelOf(1000000)).isEqualTo(30);
    }
    
    @Test
    void calcExp_shouldReturnMinimumOne_forSmallAmounts() {
        // 소액도 최소 1 경험치 보장
        assertThat(expCalculator.calcExp(1, "FRANCHISE")).isEqualTo(1);
        assertThat(expCalculator.calcExp(50, "FRANCHISE")).isEqualTo(1);
    }
    
    @Test
    void calcExp_shouldCalculateCorrectly_withDefaultWeights() {
        // 기본 가중치로 계산 (DB, yml 설정 없을 때)
        // FRANCHISE: 1.0, LOCAL: 1.5, MARKET: 2.0
        
        assertThat(expCalculator.calcExp(10000, "FRANCHISE")).isEqualTo(100); // 10000 * 1.0 / 100
        assertThat(expCalculator.calcExp(10000, "LOCAL")).isEqualTo(150);     // 10000 * 1.5 / 100
        assertThat(expCalculator.calcExp(10000, "MARKET")).isEqualTo(200);    // 10000 * 2.0 / 100
        
        // null/빈 카테고리는 기본 1.0 적용
        assertThat(expCalculator.calcExp(10000, null)).isEqualTo(100);
        assertThat(expCalculator.calcExp(10000, "")).isEqualTo(100);
        assertThat(expCalculator.calcExp(10000, "UNKNOWN")).isEqualTo(100);
    }
}