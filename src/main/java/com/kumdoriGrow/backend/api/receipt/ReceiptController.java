package com.kumdoriGrow.backend.api.receipt;

import com.kumdoriGrow.backend.api.receipt.dto.CreateReceiptReq;
import com.kumdoriGrow.backend.api.receipt.dto.CreateReceiptRes;
import com.kumdoriGrow.backend.api.receipt.dto.ReceiptResponse;
import com.kumdoriGrow.backend.api.receipt.dto.XpRes;
import com.kumdoriGrow.backend.domain.receipt.Receipt;
import com.kumdoriGrow.backend.domain.receipt.ReceiptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/receipts")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    // (1) OCR 파일 파싱: /api/receipts/parse  (멀티파트)
    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReceiptResponse parse(@RequestPart("file") MultipartFile file) {
        return receiptService.process(file);
    }

    // (2) 영수증 등록 + 경험치 지급: /api/receipts  (JSON)
    @PostMapping
    public CreateReceiptRes create(@Valid @RequestBody CreateReceiptReq req) {
        return receiptService.create(req);
    }

    // (3) 유저 누적 경험치/레벨: /api/users/{userId}/xp
    @GetMapping("/users/{userId}/xp")
    public XpRes xp(@PathVariable long userId) {
        return receiptService.getXp(userId);
    }

    // (4) 유저 영수증 목록(최근순): /api/users/{userId}/receipts
    @GetMapping("/users/{userId}/receipts")
    public Page<Receipt> list(@PathVariable long userId,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int size) {
        return receiptService.list(userId, page, size);
    }

}
