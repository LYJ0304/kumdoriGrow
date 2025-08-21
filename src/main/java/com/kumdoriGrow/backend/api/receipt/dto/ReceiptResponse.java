package com.kumdoriGrow.backend.api.receipt.dto;

public record ReceiptResponse(
        String storeName,
        Integer totalPrice,
        String rawText,
        Double confidence
) {}