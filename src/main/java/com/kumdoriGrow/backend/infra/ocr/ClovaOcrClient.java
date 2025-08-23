package com.kumdoriGrow.backend.infra.ocr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kumdoriGrow.backend.config.OcrProperties;
import com.kumdoriGrow.backend.infra.ocr.dto.OcrResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClovaOcrClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper om = new ObjectMapper();
    private final OcrProperties props;

    public OcrResult request(MultipartFile file) {
        // OCR 설정이 없으면 빈 결과 반환 (null-safe 처리)
        if (!props.enabled() || props.apiUrl().isEmpty() || props.apiKey().isEmpty()) {
            log.warn("[ClovaOcrClient] OCR is disabled or not configured properly. enabled={}, apiUrl={}, apiKey={}", 
                props.enabled(), props.apiUrl().isEmpty() ? "empty" : "configured", props.apiKey().isEmpty() ? "empty" : "configured");
            return new OcrResult(); // 빈 결과 객체 반환
        }
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.add("X-OCR-SECRET", props.apiKey());

            log.info("[ClovaOcrClient] Using OCR URL = {}", props.apiUrl());

            Map<String, Object> msg = new HashMap<>();
            msg.put("version", "V2");
            msg.put("requestId", UUID.randomUUID().toString());
            msg.put("timestamp", System.currentTimeMillis());
            msg.put("images", List.of(Map.of("format", guessExt(file), "name", "receipt")));

            HttpHeaders jsonHeader = new HttpHeaders();
            jsonHeader.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> messagePart = new HttpEntity<>(om.writeValueAsString(msg), jsonHeader);

            HttpHeaders fileHeader = new HttpHeaders();
            fileHeader.setContentType(MediaType.parseMediaType(
                    file.getContentType() != null ? file.getContentType() : MediaType.IMAGE_JPEG_VALUE));
            HttpEntity<Resource> filePart = new HttpEntity<>(new ByteArrayResource(file.getBytes()) {
                @Override public String getFilename() { return "upload." + guessExt(file); }
            }, fileHeader);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("message", messagePart);
            body.add("file", filePart);

            HttpEntity<MultiValueMap<String, Object>> req = new HttpEntity<>(body, headers);
            ResponseEntity<String> res = restTemplate.postForEntity(props.apiUrl(), req, String.class);

            if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
                throw new IllegalStateException("OCR API 실패: " + res.getStatusCode());
            }
            return om.readValue(res.getBody(), OcrResult.class);
        } catch (Exception e) {
            log.error("Clova OCR 호출 실패", e);
            throw new IllegalStateException("Clova OCR 호출 실패", e);
        }
    }

    private String guessExt(MultipartFile f) {
        String c = f.getContentType();
        if (c == null) return "jpg";
        if (c.contains("png")) return "png";
        if (c.contains("jpeg") || c.contains("jpg")) return "jpg";
        return "jpg";
    }
}
