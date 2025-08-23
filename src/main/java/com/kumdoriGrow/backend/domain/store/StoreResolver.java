package com.kumdoriGrow.backend.domain.store;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreResolver {
    
    private final StoreRepository storeRepository;
    private final StoreAliasRepository storeAliasRepository;
    private final NameNormalizer nameNormalizer;
    
    private static final double FUZZY_THRESHOLD = 0.88;
    
    /**
     * OCR 텍스트를 분석하여 가장 적합한 가게를 찾는다
     */
    public StoreMatchResult resolve(String ocrRawText) {
        if (ocrRawText == null || ocrRawText.trim().isEmpty()) {
            return StoreMatchResult.noMatch();
        }
        
        log.debug("Resolving store from OCR text: {}", ocrRawText);
        
        // 1. OCR 텍스트에서 가능한 가게명들 추출
        String[] possibleStoreNames = nameNormalizer.extractPossibleStoreNames(ocrRawText);
        
        List<StoreMatchResult> candidates = new ArrayList<>();
        
        // 2. 각 가능한 가게명에 대해 매칭 시도
        for (String possibleName : possibleStoreNames) {
            // 2-1. 정확한 매칭 시도
            StoreMatchResult exactMatch = findExactMatch(possibleName);
            if (exactMatch.isMatched()) {
                candidates.add(exactMatch);
            }
            
            // 2-2. 부분 매칭 시도
            List<StoreMatchResult> partialMatches = findPartialMatches(possibleName);
            candidates.addAll(partialMatches);
            
            // 2-3. 퍼지 매칭 시도 (정확한 매칭이 없는 경우만)
            if (exactMatch.getStore() == null) {
                List<StoreMatchResult> fuzzyMatches = findFuzzyMatches(possibleName);
                candidates.addAll(fuzzyMatches);
            }
        }
        
        // 3. 최적의 후보 선택
        return selectBestMatch(candidates);
    }
    
    private StoreMatchResult findExactMatch(String possibleName) {
        String normalized = nameNormalizer.normalize(possibleName);
        
        // 가게명 정확 매칭
        Optional<Store> storeByName = storeRepository.findByNormalizedName(normalized);
        if (storeByName.isPresent()) {
            return StoreMatchResult.exactMatch(storeByName.get(), possibleName, 
                StoreMatchResult.MatchType.EXACT_STORE_NAME);
        }
        
        // 별칭 정확 매칭
        Optional<StoreAlias> storeByAlias = storeAliasRepository.findByNormalizedAlias(normalized);
        if (storeByAlias.isPresent()) {
            Store store = storeRepository.findById(storeByAlias.get().getStoreId()).orElse(null);
            if (store != null) {
                return StoreMatchResult.exactMatch(store, possibleName, 
                    StoreMatchResult.MatchType.EXACT_ALIAS);
            }
        }
        
        return StoreMatchResult.noMatch();
    }
    
    private List<StoreMatchResult> findPartialMatches(String possibleName) {
        String normalized = nameNormalizer.normalize(possibleName);
        List<StoreMatchResult> matches = new ArrayList<>();
        
        // 가게명 부분 매칭
        List<Store> storesByPartialName = storeRepository.findByNormalizedNameContaining(normalized);
        for (Store store : storesByPartialName) {
            matches.add(new StoreMatchResult(store, possibleName, 0.95, 
                StoreMatchResult.MatchType.PARTIAL_STORE_NAME));
        }
        
        // 별칭 부분 매칭
        List<StoreAlias> aliasesByPartialName = storeAliasRepository.findByNormalizedAliasContaining(normalized);
        for (StoreAlias alias : aliasesByPartialName) {
            Store store = storeRepository.findById(alias.getStoreId()).orElse(null);
            if (store != null) {
                matches.add(new StoreMatchResult(store, possibleName, 0.93, 
                    StoreMatchResult.MatchType.PARTIAL_ALIAS));
            }
        }
        
        return matches;
    }
    
    private List<StoreMatchResult> findFuzzyMatches(String possibleName) {
        String normalized = nameNormalizer.normalize(possibleName);
        List<StoreMatchResult> matches = new ArrayList<>();
        
        // 모든 가게와 퍼지 매칭
        List<Store> allStores = storeRepository.findAll();
        for (Store store : allStores) {
            double similarity = calculateSimilarity(normalized, store.getNormalizedName());
            if (similarity >= FUZZY_THRESHOLD) {
                matches.add(StoreMatchResult.fuzzyMatch(store, possibleName, similarity));
            }
        }
        
        // 모든 별칭과 퍼지 매칭
        List<StoreAlias> allAliases = storeAliasRepository.findAll();
        for (StoreAlias alias : allAliases) {
            double similarity = calculateSimilarity(normalized, alias.getNormalizedAlias());
            if (similarity >= FUZZY_THRESHOLD) {
                Store store = storeRepository.findById(alias.getStoreId()).orElse(null);
                if (store != null) {
                    matches.add(StoreMatchResult.fuzzyMatch(store, possibleName, similarity));
                }
            }
        }
        
        return matches;
    }
    
    private StoreMatchResult selectBestMatch(List<StoreMatchResult> candidates) {
        if (candidates.isEmpty()) {
            return StoreMatchResult.noMatch();
        }
        
        // 신뢰도 순으로 정렬
        candidates.sort((a, b) -> {
            // 1. 신뢰도 우선
            int confidenceCompare = Double.compare(b.getConfidence(), a.getConfidence());
            if (confidenceCompare != 0) {
                return confidenceCompare;
            }
            
            // 2. 매칭된 텍스트 길이 우선 (더 긴 것이 더 구체적)
            int lengthCompare = Integer.compare(b.getMatchedText().length(), a.getMatchedText().length());
            if (lengthCompare != 0) {
                return lengthCompare;
            }
            
            // 3. 브랜드가 있는 것 우선
            boolean aHasBrand = a.getStore().getBrand() != null;
            boolean bHasBrand = b.getStore().getBrand() != null;
            return Boolean.compare(bHasBrand, aHasBrand);
        });
        
        StoreMatchResult bestMatch = candidates.get(0);
        log.debug("Best match found: {} (confidence: {}, type: {})", 
            bestMatch.getStore().getName(), bestMatch.getConfidence(), bestMatch.getMatchType());
        
        return bestMatch;
    }
    
    /**
     * Jaro-Winkler 유사도 계산 (간단 구현)
     */
    private double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        if (s1.equals(s2)) return 1.0;
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;
        
        // 간단한 Levenshtein 거리 기반 유사도
        int maxLength = Math.max(s1.length(), s2.length());
        int distance = levenshteinDistance(s1, s2);
        return 1.0 - (double) distance / maxLength;
    }
    
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(dp[i - 1][j], Math.min(dp[i][j - 1], dp[i - 1][j - 1])) + 1;
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
}