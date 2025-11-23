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

    // 비속어 및 부적절한 표현 사전 (필터링용)
    private static final Set<String> PROFANITY_WORDS = new HashSet<>(Arrays.asList(
            // 비속어 (일부 예시 - 실제로는 더 많은 단어가 필요)
            "시발", "개새끼", "병신", "좆", "지랄", "미친", "미친놈", "미친년",
            "개같은", "개새", "새끼", "씨발", "좆같은", "지랄", "개지랄",
            "병신", "멍청이", "바보", "등신", "호구", "찐따"
    ));

    // 구어체 정규화 맵 (구어체 -> 표준어)
    private static final Map<String, String> COLLOQUIAL_NORMALIZATION = new HashMap<String, String>() {{
        // 축약형 대명사
        put("그거", "그것");
        put("이거", "이것");
        put("저거", "저것");
        put("그게", "그것이");
        put("이게", "이것이");
        put("저게", "저것이");
        
        // 구어체 동사/형용사
        put("됐어", "되었다");
        put("했어", "했다");
        put("있어", "있다");
        put("없어", "없다");
        put("좋아", "좋다");
        put("나빠", "나쁘다");
        put("많아", "많다");
        put("적어", "적다");
        
        // 구어체 부사/형용사
        put("안됨", "안됨"); // 그대로 유지
        put("안됐어", "안되었다");
        put("안했어", "안했다");
        
        // 줄임말
        put("됨", "됨"); // 그대로 유지
        put("안됨", "안됨");
    }};

    /**
     * 반복 문자 정규화 (예: "너무너무너무" -> "너무")
     *
     * @param text 원본 텍스트
     * @return 정규화된 텍스트
     */
    private String normalizeRepeatedChars(String text) {
        // 같은 한글 단어가 2번 이상 반복되는 경우 1번으로 축약
        // 예: "너무너무너무" -> "너무", "좋아좋아" -> "좋아"
        return text.replaceAll("([가-힣]{2,})\\1+", "$1");
    }

    /**
     * 구어체 정규화 (축약형, 줄임말 등을 표준어로 변환)
     *
     * @param text 원본 텍스트
     * @return 정규화된 텍스트
     */
    private String normalizeColloquial(String text) {
        String normalized = text;
        
        // 구어체 정규화 맵을 사용하여 변환
        for (Map.Entry<String, String> entry : COLLOQUIAL_NORMALIZATION.entrySet()) {
            normalized = normalized.replace(entry.getKey(), entry.getValue());
        }
        
        return normalized;
    }

    /**
     * 비속어 필터링 (비속어를 공백으로 대체)
     *
     * @param text 원본 텍스트
     * @return 필터링된 텍스트
     */
    private String filterProfanity(String text) {
        String filtered = text;
        
        for (String profanity : PROFANITY_WORDS) {
            // 비속어를 공백으로 대체 (대소문자 구분 없이)
            filtered = filtered.replaceAll("(?i)" + Pattern.quote(profanity), " ");
        }
        
        return filtered;
    }

    /**
     * 텍스트 전처리: 특수문자, 이모지 제거 및 기본 정제
     * 구어체 정규화 및 비속어 필터링 포함
     *
     * @param text 원본 텍스트
     * @return 정제된 텍스트
     */
    public String preprocessText(String text) {
        return preprocessText(text, true, true);
    }

    /**
     * 텍스트 전처리 (옵션 포함)
     *
     * @param text 원본 텍스트
     * @param normalizeColloquial 구어체 정규화 여부
     * @param filterProfanity 비속어 필터링 여부
     * @return 정제된 텍스트
     */
    public String preprocessText(String text, boolean normalizeColloquial, boolean filterProfanity) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        String cleaned = text;

        // 1. 반복 문자 정규화 (예: "너무너무너무" -> "너무")
        cleaned = normalizeRepeatedChars(cleaned);

        // 2. 구어체 정규화 (옵션)
        if (normalizeColloquial) {
            cleaned = normalizeColloquial(cleaned);
        }

        // 3. 비속어 필터링 (옵션)
        if (filterProfanity) {
            cleaned = filterProfanity(cleaned);
        }

        // 4. 이모지 및 특수문자 제거 (한글과 공백만 남김)
        cleaned = EMOJI_AND_SPECIAL_CHAR_PATTERN.matcher(cleaned).replaceAll(" ");

        // 5. 연속된 공백을 하나로 통합
        cleaned = cleaned.replaceAll("\\s+", " ");

        // 6. 앞뒤 공백 제거
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
        return filterStopWords(words, true);
    }

    /**
     * 불용어 필터링 (비속어 필터링 옵션 포함)
     *
     * @param words 단어 리스트
     * @param filterProfanity 비속어 필터링 여부
     * @return 불용어가 제거된 단어 리스트
     */
    public List<String> filterStopWords(List<String> words, boolean filterProfanity) {
        if (words == null || words.isEmpty()) {
            return Collections.emptyList();
        }

        return words.stream()
                .filter(word -> {
                    // 불용어 제거
                    if (STOP_WORDS.contains(word)) {
                        return false;
                    }
                    // 비속어 필터링 (옵션)
                    if (filterProfanity && PROFANITY_WORDS.contains(word)) {
                        return false;
                    }
                    return true;
                })
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

