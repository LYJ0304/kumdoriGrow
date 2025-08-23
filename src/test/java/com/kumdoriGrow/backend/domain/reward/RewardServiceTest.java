package com.kumdoriGrow.backend.domain.reward;

import com.kumdoriGrow.backend.domain.receipt.Receipt;
import com.kumdoriGrow.backend.domain.receipt.ReceiptRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RewardServiceTest {
    
    @Mock
    private ReceiptRepository receiptRepository;
    
    private RewardService rewardService;
    private ObjectMapper objectMapper = new ObjectMapper();
    
    @BeforeEach
    void setUp() {
        rewardService = new RewardService(receiptRepository, objectMapper);
    }
    
    @Test
    void openBoxIfEligible_shouldOpenBox_whenLevelIsMultipleOfFive() {
        // Given
        Long userId = 999L;
        int newLevel = 5;
        when(receiptRepository.existsByUserIdAndStatusAndOcrRawContaining(
                userId, "REWARD", "\"level\":5")).thenReturn(false);
        
        // When
        Optional<RewardService.BoxOpenResult> result = rewardService.openBoxIfEligible(userId, newLevel, 12345L);
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().level()).isEqualTo(5);
        assertThat(result.get().points()).isGreaterThan(0);
        
        // Verify receipt was saved
        ArgumentCaptor<Receipt> receiptCaptor = ArgumentCaptor.forClass(Receipt.class);
        verify(receiptRepository).save(receiptCaptor.capture());
        
        Receipt savedReceipt = receiptCaptor.getValue();
        assertThat(savedReceipt.getUserId()).isEqualTo(userId);
        assertThat(savedReceipt.getStoreName()).isEqualTo("POINT_BOX");
        assertThat(savedReceipt.getCategoryCode()).isEqualTo("REWARD");
        assertThat(savedReceipt.getStatus()).isEqualTo("REWARD");
        assertThat(savedReceipt.getExpAwarded()).isEqualTo(0);
        assertThat(savedReceipt.getTotalAmount()).isEqualTo((long) result.get().points());
    }
    
    @Test
    void openBoxIfEligible_shouldNotOpenBox_whenLevelIsNotMultipleOfFive() {
        // Given
        Long userId = 999L;
        int newLevel = 3;
        
        // When
        Optional<RewardService.BoxOpenResult> result = rewardService.openBoxIfEligible(userId, newLevel, null);
        
        // Then
        assertThat(result).isEmpty();
        verify(receiptRepository, never()).save(any());
    }
    
    @Test
    void openBoxIfEligible_shouldNotOpenBox_whenAlreadyRewarded() {
        // Given
        Long userId = 999L;
        int newLevel = 10;
        when(receiptRepository.existsByUserIdAndStatusAndOcrRawContaining(
                userId, "REWARD", "\"level\":10")).thenReturn(true);
        
        // When
        Optional<RewardService.BoxOpenResult> result = rewardService.openBoxIfEligible(userId, newLevel, null);
        
        // Then
        assertThat(result).isEmpty();
        verify(receiptRepository, never()).save(any());
    }
    
    @Test
    void openBoxNow_shouldAlwaysOpenBox() {
        // Given
        Long userId = 999L;
        Long testSeed = 54321L;
        
        // When
        RewardService.BoxOpenResult result = rewardService.openBoxNow(userId, testSeed);
        
        // Then
        assertThat(result.points()).isGreaterThan(0);
        assertThat(result.level()).isNull(); // 강제 개봉이므로 레벨 정보 없음
        assertThat(result.roll()).isBetween(0.0, 1.0);
        
        // Verify receipt was saved
        verify(receiptRepository).save(any(Receipt.class));
    }
    
    @Test
    void getRewardProbabilities_shouldReturnCorrectProbabilities() {
        // When
        var probabilities = rewardService.getRewardProbabilities();
        
        // Then
        assertThat(probabilities).hasSize(7);
        
        // 확률 합계가 1.0인지 확인
        double totalWeight = probabilities.stream()
                .mapToDouble(RewardService.RewardWeight::weight)
                .sum();
        assertThat(totalWeight).isEqualTo(1.0, org.assertj.core.data.Offset.offset(0.001));
        
        // 각 포인트별 확률 확인
        assertThat(probabilities).anySatisfy(p -> {
            assertThat(p.points()).isEqualTo(10000);
            assertThat(p.weight()).isEqualTo(0.01);
        });
        
        assertThat(probabilities).anySatisfy(p -> {
            assertThat(p.points()).isEqualTo(50);
            assertThat(p.weight()).isEqualTo(0.40);
        });
    }
    
    @Test
    void rewardSelection_shouldBeConsistentWithSeed() {
        // Given
        Long userId = 999L;
        Long seed = 42L;
        
        // When - 같은 시드로 여러 번 실행
        RewardService.BoxOpenResult result1 = rewardService.openBoxNow(userId, seed);
        reset(receiptRepository);
        RewardService.BoxOpenResult result2 = rewardService.openBoxNow(userId, seed);
        
        // Then - 같은 결과가 나와야 함
        assertThat(result1.points()).isEqualTo(result2.points());
        assertThat(result1.roll()).isEqualTo(result2.roll());
    }
}