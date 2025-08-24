package com.kumdoriGrow.backend.domain.reward;

import com.kumdoriGrow.backend.domain.receipt.LevelUpEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 레벨업 이벤트 처리 - 포인트 박스 개봉
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LevelUpEventHandler {
    
    @Lazy
    private final RewardService rewardService;
    
    @EventListener
    public void handleLevelUp(LevelUpEvent event) {
        try {
            var boxResult = rewardService.openBoxIfEligible(
                event.userId(), 
                event.newLevel(), 
                null // 실제 운영에서는 테스트 시드 없음
            );
            
            if (boxResult.isPresent()) {
                log.info("Level-up reward processed: user={}, level={}, points={}", 
                    event.userId(), event.newLevel(), boxResult.get().points());
            }
        } catch (Exception e) {
            log.error("Failed to process level-up reward for user {} at level {}", 
                event.userId(), event.newLevel(), e);
        }
    }
}