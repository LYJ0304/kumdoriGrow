package com.kumdoriGrow.backend.domain.receipt;


import com.kumdoriGrow.backend.api.receipt.dto.ReceiptResponse;
import com.kumdoriGrow.backend.infra.ocr.ClovaOcrClient;
import com.kumdoriGrow.backend.infra.ocr.dto.OcrFieldModels;
import com.kumdoriGrow.backend.infra.ocr.dto.OcrResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReceiptService {

    private final ClovaOcrClient ocrClient;
    private final ReceiptParser parser = new ReceiptParser(); // 단순 사용이면 빈 등록 불필요

    public ReceiptResponse process(MultipartFile file) {
        OcrResult ocr = ocrClient.request(file);

        String rawText = flattenText(ocr);
        String store = parser.extractStoreName(rawText);
        Integer total = parser.extractTotalPrice(rawText);
        Double conf = avgConfidence(ocr);

        return new ReceiptResponse(store, total, rawText, conf);
    }

    private String flattenText(OcrResult ocr) {
        StringBuilder sb = new StringBuilder();
        if (ocr.getImages() == null) return "";
        for (OcrFieldModels.OcrImage img : ocr.getImages()) {
            if (img.getFields() == null) continue;
            for (OcrFieldModels.OcrField f : img.getFields()) {
                if (f.getInferText() != null) sb.append(f.getInferText()).append('\n');
            }
        }
        return sb.toString().trim();
    }

    private Double avgConfidence(OcrResult ocr) {
        if (ocr.getImages() == null) return null;
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
