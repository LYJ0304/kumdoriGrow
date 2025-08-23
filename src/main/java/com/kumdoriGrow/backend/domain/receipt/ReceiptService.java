package com.kumdoriGrow.backend.domain.receipt;


import com.kumdoriGrow.backend.api.receipt.dto.CreateReceiptReq;
import com.kumdoriGrow.backend.api.receipt.dto.CreateReceiptRes;
import com.kumdoriGrow.backend.api.receipt.dto.ReceiptResponse;
import com.kumdoriGrow.backend.api.receipt.dto.XpRes;
import com.kumdoriGrow.backend.domain.store.StoreMatchResult;
import com.kumdoriGrow.backend.domain.store.StoreResolver;
import com.kumdoriGrow.backend.infra.ocr.ClovaOcrClient;
import com.kumdoriGrow.backend.infra.ocr.dto.OcrFieldModels;
import com.kumdoriGrow.backend.infra.ocr.dto.OcrResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final ExpCalculator expCalculator;
    private final UserRepository userRepository;
    private final ClovaOcrClient ocrClient;
    private final StoreResolver storeResolver;

    // 단순 파서라면 new로 써도 되지만, 재사용/테스트 편의 위해 @Component로 빼도 OK
    private final ReceiptParser parser = new ReceiptParser();

    // OCR 텍스트 기반 자동 가게 매칭 및 경험치 계산
    @Transactional
    public CreateReceiptRes create(CreateReceiptReq req) {
        userRepository.findById(req.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + req.userId()));

        Receipt r = new Receipt();
        r.setUserId(req.userId());
        r.setTotalAmount(req.totalAmount());
        r.setImagePath(req.imagePath());
        r.setOcrRaw(req.ocrRaw());
        
        // 1. OCR 텍스트로 가게 매칭 시도
        StoreMatchResult matchResult = storeResolver.resolve(req.ocrRaw());
        
        String finalCategoryCode;
        String finalStoreName;
        
        if (matchResult.isMatched() && matchResult.getConfidence() >= 0.85) {
            // 매칭 성공: DB에서 찾은 가게 정보 사용
            log.info("Store matched: {} (confidence: {})", 
                matchResult.getStore().getName(), matchResult.getConfidence());
            
            finalStoreName = matchResult.getStore().getName();
            finalCategoryCode = matchResult.getStore().getCategoryCode();
            r.setMatchedStoreId(matchResult.getStore().getId());
            r.setStoreNameConfidence(matchResult.getConfidence());
            r.setStatus("DONE");
            
        } else {
            // 매칭 실패: 요청 데이터 사용 또는 수동 리뷰 필요
            finalStoreName = req.storeName() != null ? req.storeName() : "미확인 가게";
            finalCategoryCode = req.categoryCode();
            
            if (req.categoryCode() == null || req.categoryCode().trim().isEmpty()) {
                // 카테고리도 없으면 수동 리뷰 필요
                r.setStatus("NEED_REVIEW");
                finalCategoryCode = "LOCAL"; // 기본값
                log.warn("Store matching failed and no category provided. Receipt needs manual review.");
            } else {
                r.setStatus("DONE");
                log.info("Using provided store info: {} ({})", finalStoreName, finalCategoryCode);
            }
        }
        
        r.setStoreName(finalStoreName);
        r.setCategoryCode(finalCategoryCode);
        
        // 2. 경험치 계산
        int exp = expCalculator.calcExp(req.totalAmount(), finalCategoryCode);
        r.setExpAwarded(exp);
        
        receiptRepository.save(r);

        // 3. 사용자 총 경험치 및 레벨 계산
        long total = receiptRepository.sumExpByUser(req.userId());
        int level = expCalculator.levelOf(total);

        return new CreateReceiptRes(r.getId(), exp, total, level, 
            matchResult.isMatched() ? matchResult.getStore().getName() : null,
            matchResult.getConfidence());
    }

    // 누적 경험치/레벨
    public XpRes getXp(long userId) {
        long total = receiptRepository.sumExpByUser(userId);
        int level = expCalculator.levelOf(total);
        return new XpRes(total, level);
    }

    // 최근 영수증 목록
    public Page<Receipt> list(long userId, int page, int size) {
        return receiptRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    // OCR 처리(멀티파트)
    public ReceiptResponse process(MultipartFile file) {
        OcrResult ocr = ocrClient.request(file);   // ← 주입된 클라이언트 사용

        String rawText = flattenText(ocr);
        String store = parser.extractStoreName(rawText);
        Integer total = parser.extractTotalPrice(rawText);
        Double conf = avgConfidence(ocr);

        return new ReceiptResponse(store, total, rawText, conf);
    }

    private String flattenText(OcrResult ocr) {
        StringBuilder sb = new StringBuilder();
        if (ocr == null || ocr.getImages() == null) return "";
        for (OcrFieldModels.OcrImage img : ocr.getImages()) {
            if (img.getFields() == null) continue;
            for (OcrFieldModels.OcrField f : img.getFields()) {
                if (f.getInferText() != null) sb.append(f.getInferText()).append('\n');
            }
        }
        return sb.toString().trim();
    }

    private Double avgConfidence(OcrResult ocr) {
        if (ocr == null || ocr.getImages() == null) return null;
        double sum = 0; int cnt = 0;
        for (OcrFieldModels.OcrImage img : ocr.getImages()) {
            if (img.getFields() == null) continue;
            for (OcrFieldModels.OcrField f : img.getFields()) {
                if (f.getInferConfidence() != null) { sum += f.getInferConfidence(); cnt++; }
            }
        }
        return cnt == 0 ? null : Math.round((sum / cnt) * 100.0) / 100.0;
    }
}
