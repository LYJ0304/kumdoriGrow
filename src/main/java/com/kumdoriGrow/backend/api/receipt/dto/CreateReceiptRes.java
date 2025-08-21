package com.kumdoriGrow.backend.api.receipt.dto;

public record CreateReceiptRes(
        Long receiptId,
        int expAwarded,
        long totalExpAfter,
        int levelAfter
) {}
