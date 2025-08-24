package com.kumdoriGrow.backend.domain.receipt;

/**
 * 레벨업 이벤트 - 포인트 박스 개봉 트리거용
 */
public record LevelUpEvent(
    Long userId,
    int oldLevel,
    int newLevel,
    Long receiptId
) {}