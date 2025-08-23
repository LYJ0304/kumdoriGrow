package com.kumdoriGrow.backend.domain.store;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StoreMatchResult {
    private Store store;
    private String matchedText;
    private double confidence;
    private MatchType matchType;
    
    public enum MatchType {
        EXACT_STORE_NAME,      // 정확한 가게명 매칭
        EXACT_ALIAS,          // 정확한 별칭 매칭
        PARTIAL_STORE_NAME,   // 부분 가게명 매칭
        PARTIAL_ALIAS,        // 부분 별칭 매칭
        FUZZY_MATCH          // 퍼지 매칭
    }
    
    public static StoreMatchResult exactMatch(Store store, String matchedText, MatchType matchType) {
        return new StoreMatchResult(store, matchedText, 0.99, matchType);
    }
    
    public static StoreMatchResult fuzzyMatch(Store store, String matchedText, double similarity) {
        return new StoreMatchResult(store, matchedText, similarity, MatchType.FUZZY_MATCH);
    }
    
    public static StoreMatchResult noMatch() {
        return new StoreMatchResult(null, null, 0.0, null);
    }
    
    public boolean isMatched() {
        return store != null && confidence > 0.0;
    }
}