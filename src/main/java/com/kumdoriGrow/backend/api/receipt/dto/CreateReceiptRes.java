package com.kumdoriGrow.backend.api.receipt.dto;

public record CreateReceiptRes(
        Long receiptId,
        int expAwarded,
        long totalExpAfter,
        int levelAfter,
        String matchedStoreName,
        Double confidence
) {
    // 기존 호환성을 위한 생성자
    public CreateReceiptRes(Long receiptId, int expAwarded, long totalExpAfter, int levelAfter) {
        this(receiptId, expAwarded, totalExpAfter, levelAfter, null, null);
    }
}
