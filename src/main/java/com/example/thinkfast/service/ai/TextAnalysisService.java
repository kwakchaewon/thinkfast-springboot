package com.example.thinkfast.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 한국어 텍스트 분석 서비스
 * 형태소 분석 없이 정규식 기반으로 텍스트 전처리 및 키워드 추출 수행
 */
@Slf4j
@Service
public class TextAnalysisService {

    // 한글 단어 추출을 위한 정규식 패턴
    private static final Pattern KOREAN_WORD_PATTERN = Pattern.compile("[가-힣]+");
    
    // 이모지 및 특수문자 제거를 위한 패턴
    private static final Pattern EMOJI_AND_SPECIAL_CHAR_PATTERN = Pattern.compile("[^가-힣\\s]");
    
    // 한국어 불용어 리스트 (하드코딩)
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            // 조사
            "이", "가", "을", "를", "의", "에", "에서", "로", "으로", "와", "과", "도", "만", "부터", "까지",
            // 대명사
            "그", "그것", "이것", "저것", "그들", "이들", "저들", "나", "너", "우리", "당신",
            // 접속사
            "그리고", "또한", "또", "그러나", "하지만", "그런데", "그래서", "따라서", "그러므로",
            // 부사
            "매우", "아주", "너무", "정말", "진짜", "완전", "꽤", "조금", "좀", "많이", "적게",
            // 일반적인 불용어
            "것", "수", "때", "곳", "일", "등", "및", "등등", "등의", "같은", "다른", "같이",
            "있", "없", "하", "되", "되다", "하다", "있다", "없다", "되다", "하다", "이다",
            "되도록", "하도록", "이렇게", "그렇게", "저렇게", "어떻게", "왜", "언제", "어디",
            "이런", "그런", "저런", "어떤", "무엇", "누구", "어느"
    ));

    /**
     * 텍스트 전처리: 특수문자, 이모지 제거 및 기본 정제
     *
     * @param text 원본 텍스트
     * @return 정제된 텍스트
     */
    public String preprocessText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        // 1. 이모지 및 특수문자 제거 (한글과 공백만 남김)
        String cleaned = EMOJI_AND_SPECIAL_CHAR_PATTERN.matcher(text).replaceAll(" ");
        
        // 2. 연속된 공백을 하나로 통합
        cleaned = cleaned.replaceAll("\\s+", " ");
        
        // 3. 앞뒤 공백 제거
        cleaned = cleaned.trim();
        
        return cleaned;
    }

    /**
     * 정규식 기반 한글 단어 추출
     *
     * @param text 전처리된 텍스트
     * @return 추출된 단어 리스트
     */
    public List<String> extractWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> words = new ArrayList<>();
        Matcher matcher = KOREAN_WORD_PATTERN.matcher(text);
        
        while (matcher.find()) {
            String word = matcher.group();
            // 최소 2글자 이상인 단어만 추출 (1글자는 의미가 적음)
            if (word.length() >= 2) {
                words.add(word);
            }
        }
        
        return words;
    }

    /**
     * 불용어 필터링
     *
     * @param words 단어 리스트
     * @return 불용어가 제거된 단어 리스트
     */
    public List<String> filterStopWords(List<String> words) {
        if (words == null || words.isEmpty()) {
            return Collections.emptyList();
        }

        return words.stream()
                .filter(word -> !STOP_WORDS.contains(word))
                .collect(Collectors.toList());
    }

    /**
     * 텍스트에서 키워드 추출 (전처리 + 단어 추출 + 불용어 제거)
     *
     * @param text 원본 텍스트
     * @return 추출된 키워드 리스트
     */
    public List<String> extractKeywords(String text) {
        // 1. 텍스트 전처리
        String preprocessed = preprocessText(text);
        
        // 2. 단어 추출
        List<String> words = extractWords(preprocessed);
        
        // 3. 불용어 제거
        List<String> keywords = filterStopWords(words);
        
        return keywords;
    }

    /**
     * 키워드 빈도수 계산
     *
     * @param keywords 키워드 리스트
     * @return 키워드와 빈도수를 포함한 Map
     */
    public Map<String, Integer> calculateWordFrequency(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Integer> frequencyMap = new HashMap<>();
        for (String keyword : keywords) {
            frequencyMap.put(keyword, frequencyMap.getOrDefault(keyword, 0) + 1);
        }
        
        return frequencyMap;
    }

    /**
     * 빈도수 기반으로 키워드 정렬 (내림차순)
     *
     * @param frequencyMap 키워드 빈도수 Map
     * @return 빈도수 순으로 정렬된 키워드 리스트
     */
    public List<Map.Entry<String, Integer>> sortByFrequency(Map<String, Integer> frequencyMap) {
        return frequencyMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());
    }

    /**
     * 상위 N개 키워드 추출
     *
     * @param keywords 키워드 리스트
     * @param topN 상위 N개
     * @return 상위 N개 키워드와 빈도수 리스트
     */
    public List<Map.Entry<String, Integer>> getTopKeywords(List<String> keywords, int topN) {
        Map<String, Integer> frequencyMap = calculateWordFrequency(keywords);
        List<Map.Entry<String, Integer>> sorted = sortByFrequency(frequencyMap);
        
        return sorted.stream()
                .limit(topN)
                .collect(Collectors.toList());
    }

    /**
     * 텍스트에서 상위 N개 키워드 추출 (전체 프로세스)
     *
     * @param text 원본 텍스트
     * @param topN 상위 N개
     * @return 상위 N개 키워드와 빈도수 리스트
     */
    public List<Map.Entry<String, Integer>> extractTopKeywords(String text, int topN) {
        List<String> keywords = extractKeywords(text);
        return getTopKeywords(keywords, topN);
    }

    /**
     * 최소 빈도수 이상인 키워드만 필터링
     *
     * @param frequencyMap 키워드 빈도수 Map
     * @param minFrequency 최소 빈도수
     * @return 필터링된 키워드 리스트
     */
    public List<Map.Entry<String, Integer>> filterByMinFrequency(
            Map<String, Integer> frequencyMap, int minFrequency) {
        return frequencyMap.entrySet().stream()
                .filter(entry -> entry.getValue() >= minFrequency)
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());
    }
}

