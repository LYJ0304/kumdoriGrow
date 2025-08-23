package com.kumdoriGrow.backend.domain.reward;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kumdoriGrow.backend.domain.receipt.Receipt;
import com.kumdoriGrow.backend.domain.receipt.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RewardService {
    
    private final ReceiptRepository receiptRepository;
    
    // ObjectMapper는 직접 생성해서 사용 (의존성 주입 문제 방지)
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // 포인트 박스 확률표 (가중치)
    private static final List<RewardWeight> REWARD_WEIGHTS = Arrays.asList(
        new RewardWeight(10000, 0.01),  // 1%
        new RewardWeight(5000, 0.02),   // 2%
        new RewardWeight(3000, 0.05),   // 5%
        new RewardWeight(1000, 0.08),   // 8%
        new RewardWeight(500, 0.14),    // 14%
        new RewardWeight(100, 0.30),    // 30%
        new RewardWeight(50, 0.40)      // 40%
    );
    
    /**
     * 레벨업 시 포인트 박스 개봉 체크 (5의 배수만)
     */
    @Transactional
    public Optional<BoxOpenResult> openBoxIfEligible(Long userId, int newLevel, Long testSeed) {
        // 5의 배수가 아니면 개봉하지 않음
        if (newLevel % 5 != 0) {
            return Optional.empty();
        }
        
        // 이미 해당 레벨로 보상을 받았는지 확인 (중복 방지)
        boolean alreadyRewarded = receiptRepository.existsByUserIdAndStatusAndOcrRawContaining(
            userId, "REWARD", "\"level\":" + newLevel
        );
        
        if (alreadyRewarded) {
            log.info("User {} already received reward for level {}", userId, newLevel);
            return Optional.empty();
        }
        
        return Optional.of(openBoxNow(userId, newLevel, testSeed));
    }
    
    /**
     * 강제로 포인트 박스 개봉 (테스트/운영점검용)
     */
    @Transactional
    public BoxOpenResult openBoxNow(Long userId, Long testSeed) {
        return openBoxNow(userId, null, testSeed);
    }
    
    private BoxOpenResult openBoxNow(Long userId, Integer level, Long testSeed) {
        // 확률 정규화
        List<NormalizedWeight> normalized = normalizeWeights(REWARD_WEIGHTS);
        
        // 난수 생성
        Random random = testSeed != null ? new Random(testSeed) : new SecureRandom();
        double roll = random.nextDouble();
        
        // 구간 선택
        int selectedPoints = selectReward(normalized, roll);
        
        // receipts 테이블에 보상 이벤트 기록
        Receipt rewardReceipt = createRewardReceipt(userId, selectedPoints, level, roll, normalized);
        receiptRepository.save(rewardReceipt);
        
        // 로깅
        if (level != null) {
            log.info("Point box opened for user {} at level {}: {} points awarded", 
                    userId, level, selectedPoints);
        } else {
            log.info("Point box opened for user {} (manual): {} points awarded", 
                    userId, selectedPoints);
        }
        
        if (log.isDebugEnabled()) {
            log.debug("Roll: {}, Selected: {} points", roll, selectedPoints);
        }
        
        return new BoxOpenResult(selectedPoints, level, roll);
    }
    
    private List<NormalizedWeight> normalizeWeights(List<RewardWeight> weights) {
        double totalWeight = weights.stream().mapToDouble(RewardWeight::weight).sum();
        List<NormalizedWeight> normalized = new ArrayList<>();
        double cumulative = 0.0;
        
        for (RewardWeight rw : weights) {
            double probability = rw.weight() / totalWeight;
            cumulative += probability;
            normalized.add(new NormalizedWeight(rw.points(), probability, cumulative));
        }
        
        return normalized;
    }
    
    private int selectReward(List<NormalizedWeight> normalized, double roll) {
        for (NormalizedWeight nw : normalized) {
            if (roll <= nw.cumulative()) {
                return nw.points();
            }
        }
        // 백업 (이론적으로 도달하지 않음)
        return normalized.get(normalized.size() - 1).points();
    }
    
    private Receipt createRewardReceipt(Long userId, int points, Integer level, double roll, 
                                      List<NormalizedWeight> normalized) {
        Receipt receipt = new Receipt();
        receipt.setUserId(userId);
        receipt.setStoreName("POINT_BOX");
        receipt.setTotalAmount((long) points);
        receipt.setCategoryCode("REWARD");
        receipt.setExpAwarded(0);
        receipt.setStatus("REWARD");
        receipt.setRecognizedAt(Instant.now());
        
        // OCR raw에 JSON 스냅샷 저장
        try {
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("type", "POINT_REWARD");
            if (level != null) {
                snapshot.put("level", level);
            }
            snapshot.put("roll", roll);
            snapshot.put("weights", REWARD_WEIGHTS);
            snapshot.put("normalized", normalized);
            snapshot.put("version", "v1");
            
            receipt.setOcrRaw(objectMapper.writeValueAsString(snapshot));
        } catch (Exception e) {
            log.warn("Failed to serialize reward snapshot", e);
            receipt.setOcrRaw("{}");
        }
        
        return receipt;
    }
    
    public List<RewardWeight> getRewardProbabilities() {
        return new ArrayList<>(REWARD_WEIGHTS);
    }
    
    // 내부 클래스들
    public record RewardWeight(int points, double weight) {}
    
    public record NormalizedWeight(int points, double probability, double cumulative) {}
    
    public record BoxOpenResult(int points, Integer level, double roll) {}
}