package com.example.thinkfast.service.ai;

import java.util.*;

/**
 * 텍스트 분석에 사용되는 상수 및 패턴 정의
 * 코드 가독성을 위해 하드코딩된 값들을 중앙 집중식으로 관리
 */
public class TextAnalysisConstants {

    // ========== 정규식 패턴 ==========
    
    /**
     * 한글 단어 추출을 위한 정규식 패턴
     */
    public static final java.util.regex.Pattern KOREAN_WORD_PATTERN = 
            java.util.regex.Pattern.compile("[가-힣]+");
    
    /**
     * 이모지 및 특수문자 제거를 위한 패턴
     */
    public static final java.util.regex.Pattern EMOJI_AND_SPECIAL_CHAR_PATTERN = 
            java.util.regex.Pattern.compile("[^가-힣\\s]");
    
    /**
     * 초성 축약어 패턴 (2개 이상의 연속된 초성)
     */
    public static final java.util.regex.Pattern INITIAL_CONSONANT_PATTERN = 
            java.util.regex.Pattern.compile("[ㄱ-ㅎ]{2,}");

    // ========== 불용어 사전 ==========
    
    /**
     * 한국어 불용어 리스트 (하드코딩)
     */
    public static final Set<String> STOP_WORDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
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
    )));

    // ========== 비속어 사전 ==========
    
    /**
     * 비속어 및 부적절한 표현 사전 (필터링용)
     */
    public static final Set<String> PROFANITY_WORDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            // 비속어 (일부 예시 - 실제로는 더 많은 단어가 필요)
            "시발", "개새끼", "병신", "좆", "지랄", "미친", "미친놈", "미친년",
            "개같은", "개새", "새끼", "씨발", "좆같은", "지랄", "개지랄",
            "병신", "멍청이", "바보", "등신", "호구", "찐따"
    )));

    // ========== 인터넷 축약어 사전 ==========
    
    /**
     * 인터넷 초성 축약어 사전 (필터링용)
     * 대부분 비속어나 부적절한 표현이므로 필터링 대상
     */
    public static final Set<String> INTERNET_SLANG_INITIALS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            // 비속어 초성 축약어
            "ㅅㅂ", "시발",
            "ㅈㄴ", "존나", "진짜나",
            "ㅈㄹ", "지랄",
            "ㅂㅅ", "병신",
            "ㅁㅊ", "미친",
            "ㄱㅅㄲ", "개새끼",
            "ㄱㅅ", "개새",
            "ㅆㄱ", "새끼",
            "ㅈㄱ", "좆같",
            "ㄱㅈㄹ", "개지랄",
            "ㅈㅅ", "좆새",
            "ㄷㅅ", "등신",
            "ㅎㄱ", "호구",
            "ㅉㄷ", "찐따",
            "ㅁㅊㄴ", "미친놈",
            "ㅁㅊㄴ", "미친년",
            // 기타 부적절한 표현
            "ㅇㅈ", "인정", // 일부는 정상적이지만 맥락에 따라 부적절할 수 있음
            "ㄱㄷ", "개돼",
            "ㅂㄹ", "병렬",
            "ㅇㅂ", "엿바꿔"
    )));

    // ========== 구어체 정규화 맵 ==========
    
    /**
     * 구어체 정규화 맵 (구어체 -> 표준어)
     */
    public static final Map<String, String> COLLOQUIAL_NORMALIZATION = createColloquialNormalizationMap();

    /**
     * 구어체 정규화 맵 생성
     */
    private static Map<String, String> createColloquialNormalizationMap() {
        Map<String, String> map = new HashMap<>();
        
        // 축약형 대명사
        map.put("그거", "그것");
        map.put("이거", "이것");
        map.put("저거", "저것");
        map.put("그게", "그것이");
        map.put("이게", "이것이");
        map.put("저게", "저것이");
        
        // 구어체 동사/형용사
        map.put("됐어", "되었다");
        map.put("했어", "했다");
        map.put("있어", "있다");
        map.put("없어", "없다");
        map.put("좋아", "좋다");
        map.put("나빠", "나쁘다");
        map.put("많아", "많다");
        map.put("적어", "적다");
        
        // 구어체 부사/형용사
        map.put("안됨", "안됨"); // 그대로 유지
        map.put("안됐어", "안되었다");
        map.put("안했어", "안했다");
        
        // 줄임말
        map.put("됨", "됨"); // 그대로 유지
        map.put("안됨", "안됨");
        
        return Collections.unmodifiableMap(map);
    }

    // 생성자 비활성화 (유틸리티 클래스)
    private TextAnalysisConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}

