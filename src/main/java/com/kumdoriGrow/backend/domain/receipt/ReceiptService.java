package com.kumdoriGrow.backend.domain.receipt;


import com.kumdoriGrow.backend.api.receipt.dto.CreateReceiptReq;
import com.kumdoriGrow.backend.api.receipt.dto.CreateReceiptRes;
import com.kumdoriGrow.backend.api.receipt.dto.ReceiptResponse;
import com.kumdoriGrow.backend.api.receipt.dto.XpRes;
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

@Service
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final ExpCalculator expCalculator;
    private final UserRepository userRepository;
    private final ClovaOcrClient ocrClient;

    // 단순 파서라면 new로 써도 되지만, 재사용/테스트 편의 위해 @Component로 빼도 OK
    private final ReceiptParser parser = new ReceiptParser();

    public ReceiptService(ReceiptRepository receiptRepository,
                          ExpCalculator expCalculator,
                          UserRepository userRepository,
                          ClovaOcrClient ocrClient) {          // ← ocrClient 주입 추가
        this.receiptRepository = receiptRepository;
        this.expCalculator = expCalculator;
        this.userRepository = userRepository;
        this.ocrClient = ocrClient;
    }

    // 금액×가중치→경험치 계산해서 저장
    @Transactional
    public CreateReceiptRes create(CreateReceiptReq req) {
        userRepository.findById(req.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + req.userId()));

        int exp = expCalculator.calcExp(req.totalAmount(), req.categoryCode());

        Receipt r = new Receipt();
        r.setUserId(req.userId());
        r.setStoreName(req.storeName());
        r.setTotalAmount(req.totalAmount());
        r.setCategoryCode(req.categoryCode());
        r.setExpAwarded(exp);
        r.setImagePath(req.imagePath());
        r.setOcrRaw(req.ocrRaw());
        r.setStatus("DONE");

        receiptRepository.save(r);

        long total = receiptRepository.sumExpByUser(req.userId());
        int level = expCalculator.levelOf(total);

        return new CreateReceiptRes(r.getId(), exp, total, level);
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
