package com.kumdoriGrow.backend.api.reward;

import com.kumdoriGrow.backend.domain.reward.RewardQueryService;
import com.kumdoriGrow.backend.domain.reward.RewardService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rewards")
@RequiredArgsConstructor
public class RewardController {
    
    private final RewardService rewardService;
    private final RewardQueryService rewardQueryService;
    
    /**
     * 사용자 포인트 요약 조회
     */
    @GetMapping("/summary")
    public ResponseEntity<RewardQueryService.UserPointSummary> getUserPointSummary(
            @RequestParam Long userId) {
        
        if (userId == null || userId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid userId");
        }
        
        RewardQueryService.UserPointSummary summary = rewardQueryService.getUserPointSummary(userId);
        return ResponseEntity.ok(summary);
    }
    
    /**
     * 보상 히스토리 조회 (페이징)
     */
    @GetMapping("/history")
    public ResponseEntity<Page<RewardQueryService.RewardHistory>> getRewardHistory(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        if (userId == null || userId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid userId");
        }
        
        if (page < 0 || size <= 0 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page parameters");
        }
        
        Page<RewardQueryService.RewardHistory> history = 
                rewardQueryService.getRewardHistory(userId, page, size);
        return ResponseEntity.ok(history);
    }
    
    /**
     * 포인트 박스 확률표 조회
     */
    @GetMapping("/probabilities")
    public ResponseEntity<Map<String, Object>> getRewardProbabilities() {
        List<RewardService.RewardWeight> probabilities = rewardService.getRewardProbabilities();
        
        // 총 가중치 계산
        double totalWeight = probabilities.stream()
                .mapToDouble(RewardService.RewardWeight::weight)
                .sum();
        
        // 응답 데이터 구성
        Map<String, Object> response = Map.of(
            "probabilities", probabilities,
            "totalWeight", totalWeight,
            "note", "Actual probability = weight / totalWeight"
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 포인트 박스 강제 개봉 (테스트 전용)
     */
    @PostMapping("/open")
    @ConditionalOnProperty(name = "spring.profiles.active", havingValue = "test")
    public ResponseEntity<RewardService.BoxOpenResult> openRewardBox(
            @RequestBody OpenBoxRequest request) {
        
        if (request.userId() == null || request.userId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid userId");
        }
        
        try {
            RewardService.BoxOpenResult result = rewardService.openBoxNow(
                    request.userId(), request.testSeed());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Failed to open reward box");
        }
    }
    
    public record OpenBoxRequest(Long userId, Long testSeed) {}
}