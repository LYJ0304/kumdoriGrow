package com.kumdoriGrow.backend.domain.receipt;


import com.kumdoriGrow.backend.api.receipt.dto.CreateReceiptReq;
import com.kumdoriGrow.backend.api.receipt.dto.CreateReceiptRes;
import com.kumdoriGrow.backend.api.receipt.dto.ReceiptResponse;
import com.kumdoriGrow.backend.api.receipt.dto.XpRes;
import com.kumdoriGrow.backend.config.OcrProperties;
// import com.kumdoriGrow.backend.domain.reward.RewardService;
import com.kumdoriGrow.backend.domain.store.StoreMatchResult;
import com.kumdoriGrow.backend.domain.store.StoreResolver;
import com.kumdoriGrow.backend.domain.user.UserRepository;
import com.kumdoriGrow.backend.infra.ocr.ClovaOcrClient;
import com.kumdoriGrow.backend.infra.ocr.dto.OcrResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final ExpCalculator expCalculator;
    private final UserRepository userRepository;
    private final StoreResolver storeResolver;
    private final ClovaOcrClient ocrClient;
    private final OcrProperties ocrProperties;
    private final ApplicationEventPublisher eventPublisher;

    // OCR 텍스트 기반 자동 가게 매칭 및 경험치 계산
    @Transactional
    public CreateReceiptRes create(CreateReceiptReq req) {
        // 사용자 존재 확인
        userRepository.findById(req.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // 카테고리 유효성 검사
        if (req.categoryCode() != null && !isValidCategoryCode(req.categoryCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid category code: " + req.categoryCode());
        }

        Receipt r = new Receipt();
        r.setUserId(req.userId());
        r.setTotalAmount(req.totalAmount());
        r.setImagePath(req.imagePath());
        r.setOcrRaw(req.ocrRaw());
        
        try {
            // 1. OCR 텍스트로 가게 매칭 시도
            StoreMatchResult matchResult = safeResolveStore(req.ocrRaw());
            
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
                
                if (req.categoryCode() == null || req.categoryCode().trim().isEmpty()) {
                    // 카테고리도 없으면 수동 리뷰 필요
                    r.setStatus("NEED_REVIEW");
                    finalCategoryCode = null; // null 허용
                    log.warn("Store matching failed and no category provided. Receipt needs manual review.");
                } else {
                    finalCategoryCode = req.categoryCode();
                    r.setStatus("DONE");
                    log.info("Using provided store info: {} ({})", finalStoreName, finalCategoryCode);
                }
            }
            
            r.setStoreName(finalStoreName);
            r.setCategoryCode(finalCategoryCode);
            
            // 2. 경험치 계산 (NPE 방지)
            int exp = safeCalculateExp(req.totalAmount(), finalCategoryCode);
            r.setExpAwarded(exp);
            
            receiptRepository.save(r);

            // 3. 사용자 총 경험치 및 레벨 계산
            long oldTotal = receiptRepository.sumExpByUser(req.userId()) - exp; // 이전 경험치
            long newTotal = oldTotal + exp; // 새 경험치
            int oldLevel = expCalculator.levelOf(oldTotal);
            int newLevel = expCalculator.levelOf(newTotal);
            
            // 4. 레벨업 시 이벤트 발행 (포인트 박스 개봉 트리거)
            if (newLevel > oldLevel) {
                eventPublisher.publishEvent(new LevelUpEvent(req.userId(), oldLevel, newLevel, r.getId()));
            }

            return new CreateReceiptRes(r.getId(), exp, newTotal, newLevel, 
                matchResult.isMatched() ? matchResult.getStore().getName() : null,
                matchResult.getConfidence());
                
        } catch (ResponseStatusException e) {
            throw e; // HTTP 상태 예외는 그대로 전파
        } catch (Exception e) {
            log.error("Unexpected error while creating receipt", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Receipt creation failed");
        }
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
        // 1) 업로드 파일 정보 로깅
        String filename = file.getOriginalFilename();
        long fileSize = file.getSize();
        String contentType = file.getContentType();
        
        log.info("[OCR] File received - filename: {}, size: {} bytes, contentType: {}", 
                filename, fileSize, contentType);
        
        // 2) 파일 유효성 검사
        if (file.isEmpty()) {
            log.warn("[OCR] Empty file uploaded");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty or unsupported image");
        }
        
        if (contentType == null || !isValidImageType(contentType)) {
            log.warn("[OCR] Unsupported content type: {}", contentType);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty or unsupported image");
        }
        
        // 3) OCR 비활성화 시 더미 응답 반환
        if (!ocrProperties.enabled()) {
            log.info("[OCR] OCR is disabled, returning dummy response");
            return new ReceiptResponse("더미_가게명", 5000, "더미_OCR_텍스트", 0.95);
        }
        
        try {
            // 4) OCR 호출 전 로깅
            String endpoint = ocrProperties.apiUrl();
            boolean hasSecret = ocrProperties.apiKey() != null && !ocrProperties.apiKey().isEmpty();
            String payloadFormat = "multipart";
            long bytesLength = file.getSize();
            
            log.info("[OCR] Calling OCR API - endpoint: {}, hasSecret: {}, payloadFormat: {}, bytes: {}",
                    endpoint, hasSecret, payloadFormat, bytesLength);
            
            // 5) OCR 실행
            OcrResult ocrResult = ocrClient.request(file);
            String ocrText = extractOcrText(ocrResult);
            
            log.info("[OCR] OCR processing completed successfully");
            
            // 임시 응답 (실제 가게 정보 파싱은 추후 구현)
            return new ReceiptResponse("OCR_파싱됨", 0, ocrText, 1.0);
            
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // 6) 4xx 에러 처리
            String responseBody = e.getResponseBodyAsString();
            log.error("[OCR] OCR API returned 4xx error: {}, Response: {}", e.getStatusCode(), responseBody);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OCR request invalid");
            
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            // 7) 5xx 에러 처리
            String responseBody = e.getResponseBodyAsString();
            log.error("[OCR] OCR API returned 5xx error: {}, Response: {}", e.getStatusCode(), responseBody);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "OCR service temporarily unavailable");
            
        } catch (Exception e) {
            // 8) 기타 예외 처리
            log.error("[OCR] Unexpected error during OCR processing", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not process image");
        }
    }


    private boolean isValidCategoryCode(String categoryCode) {
        if (categoryCode == null || categoryCode.trim().isEmpty()) {
            return false;
        }
        String code = categoryCode.trim().toUpperCase();
        return code.equals("FRANCHISE") || code.equals("LOCAL") || code.equals("MARKET");
    }

    private StoreMatchResult safeResolveStore(String ocrRawText) {
        try {
            return storeResolver.resolve(ocrRawText);
        } catch (Exception e) {
            log.warn("Store resolution failed, proceeding with manual review", e);
            return StoreMatchResult.noMatch();
        }
    }

    private int safeCalculateExp(long amount, String categoryCode) {
        try {
            return expCalculator.calcExp(amount, categoryCode);
        } catch (Exception e) {
            log.warn("Experience calculation failed, using default value", e);
            // 기본 경험치 계산 (amount / 100, 최소 1)
            return Math.max(1, (int) (amount / 100));
        }
    }
    
    private boolean isValidImageType(String contentType) {
        return contentType.startsWith("image/") && 
               (contentType.contains("jpeg") || contentType.contains("jpg") || 
                contentType.contains("png") || contentType.contains("gif") ||
                contentType.contains("bmp") || contentType.contains("webp"));
    }
    
    private String extractOcrText(OcrResult ocrResult) {
        if (ocrResult == null || ocrResult.getImages() == null || ocrResult.getImages().isEmpty()) {
            return "OCR 텍스트 추출 실패";
        }
        
        StringBuilder text = new StringBuilder();
        ocrResult.getImages().forEach(image -> {
            if (image.getFields() != null) {
                image.getFields().forEach(field -> {
                    if (field.getInferText() != null && !field.getInferText().trim().isEmpty()) {
                        text.append(field.getInferText()).append("\n");
                    }
                });
            }
        });
        
        return text.length() > 0 ? text.toString().trim() : "OCR 텍스트 없음";
    }
}
