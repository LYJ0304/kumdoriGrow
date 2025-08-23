package com.kumdoriGrow.backend.domain.store;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class NameNormalizer {
    
    private static final Pattern COMPANY_SUFFIX_PATTERN = Pattern.compile("\\(주\\)|㈜|\\bCorp\\b|\\bInc\\b|\\bLtd\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[^가-힣a-zA-Z0-9]");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    
    public String normalize(String storeName) {
        if (storeName == null || storeName.trim().isEmpty()) {
            return "";
        }
        
        String normalized = storeName.trim();
        
        // 1. 소문자로 변환
        normalized = normalized.toLowerCase();
        
        // 2. 회사 접미사 제거 (주), ㈜, Corp, Inc, Ltd 등
        normalized = COMPANY_SUFFIX_PATTERN.matcher(normalized).replaceAll("");
        
        // 3. 특수문자 및 공백을 언더스코어로 변환
        normalized = SPECIAL_CHARS_PATTERN.matcher(normalized).replaceAll("_");
        
        // 4. 연속된 언더스코어를 하나로 합침
        normalized = normalized.replaceAll("_{2,}", "_");
        
        // 5. 앞뒤 언더스코어 제거
        normalized = normalized.replaceAll("^_+|_+$", "");
        
        return normalized;
    }
    
    /**
     * OCR 텍스트에서 가게명으로 추정되는 부분들을 추출
     */
    public String[] extractPossibleStoreNames(String ocrRawText) {
        if (ocrRawText == null || ocrRawText.trim().isEmpty()) {
            return new String[0];
        }
        
        // OCR 텍스트를 줄바꿈과 공백으로 분리
        String[] lines = ocrRawText.split("\\r?\\n|\\s{2,}");
        
        // 각 줄을 정규화하여 반환 (빈 문자열 제외)
        return java.util.Arrays.stream(lines)
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .filter(line -> line.length() >= 2) // 너무 짧은 것 제외
            .filter(line -> !isNumericOnly(line)) // 숫자만 있는 것 제외
            .limit(10) // 상위 10개만
            .toArray(String[]::new);
    }
    
    private boolean isNumericOnly(String text) {
        return text.matches("^[0-9,.:/-]+$");
    }
    
    /**
     * 두 문자열의 포함 관계 확인 (정규화된 문자열 기준)
     */
    public boolean contains(String haystack, String needle) {
        if (haystack == null || needle == null) {
            return false;
        }
        
        String normalizedHaystack = normalize(haystack);
        String normalizedNeedle = normalize(needle);
        
        return normalizedHaystack.contains(normalizedNeedle);
    }
}