package com.kumdoriGrow.backend.api.receipt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateReceiptReq(
        @NotNull Long userId,
        @NotBlank @Size(max=255) String storeName,
        @NotNull @PositiveOrZero Long totalAmount,
        @NotBlank String categoryCode,
        String imagePath,
        String ocrRaw
) {}
