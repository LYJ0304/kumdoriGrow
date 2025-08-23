package com.kumdoriGrow.backend.domain.reward;

import com.kumdoriGrow.backend.domain.receipt.Receipt;
import com.kumdoriGrow.backend.domain.receipt.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RewardQueryService {
    
    private final ReceiptRepository receiptRepository;
    
    /**
     * 사용자 포인트 요약 조회
     */
    public UserPointSummary getUserPointSummary(Long userId) {
        // 일반 영수증에서 얻은 총 경험치
        Long totalExp = receiptRepository.sumExpByUser(userId);
        if (totalExp == null) totalExp = 0L;
        
        // 보상으로 받은 총 포인트 (status='REWARD'인 receipts의 total_amount 합계)
        Long totalRewardPoints = receiptRepository.findByUserIdAndStatus(userId, "REWARD")
                .stream()
                .mapToLong(Receipt::getTotalAmount)
                .sum();
        
        // 보상 받은 횟수
        long rewardCount = receiptRepository.countByUserIdAndStatus(userId, "REWARD");
        
        return new UserPointSummary(totalExp, totalRewardPoints, rewardCount);
    }
    
    /**
     * 보상 히스토리 조회 (페이징)
     */
    public Page<RewardHistory> getRewardHistory(Long userId, int page, int size) {
        Page<Receipt> rewardReceipts = receiptRepository.findByUserIdAndStatusOrderByRecognizedAtDesc(
                userId, "REWARD", PageRequest.of(page, size));
        
        return rewardReceipts.map(this::toRewardHistory);
    }
    
    private RewardHistory toRewardHistory(Receipt receipt) {
        return new RewardHistory(
                receipt.getId(),
                receipt.getTotalAmount().intValue(), // 포인트
                receipt.getRecognizedAt(),
                receipt.getOcrRaw() // JSON 스냅샷 (level, roll 등 포함)
        );
    }
    
    // DTO 클래스들
    public record UserPointSummary(long totalExp, long totalRewardPoints, long rewardCount) {}
    
    public record RewardHistory(Long id, int points, java.time.Instant receivedAt, String snapshot) {}
}