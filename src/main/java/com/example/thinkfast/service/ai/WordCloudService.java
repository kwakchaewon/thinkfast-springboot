package com.example.thinkfast.service.ai;

import com.example.thinkfast.domain.ai.WordCloud;
import com.example.thinkfast.domain.survey.Question;
import com.example.thinkfast.domain.survey.Response;
import com.example.thinkfast.dto.ai.WordCloudDto;
import com.example.thinkfast.dto.ai.WordCloudResponseDto;
import com.example.thinkfast.exception.AiServiceException;
import com.example.thinkfast.repository.ai.WordCloudRepository;
import com.example.thinkfast.repository.survey.QuestionRepository;
import com.example.thinkfast.repository.survey.ResponseRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 워드클라우드 생성 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WordCloudService {

    private final ResponseRepository responseRepository;
    private final QuestionRepository questionRepository;
    private final TextAnalysisService textAnalysisService;
    private final WordCloudRepository wordCloudRepository;
    private final ObjectMapper objectMapper;
    private final GeminiApiService geminiApiService;

    @Value("${llm.wordcloud.enabled:true}")
    private boolean llmWordCloudEnabled;

    /**
     * 질문별 워드클라우드 생성 (Gemini API 기반, 실패 시 Java 기반 폴백)
     *
     * @param questionId 질문 ID
     * @param topN 상위 N개 키워드 (기본값: 50)
     * @return 워드클라우드 응답 DTO
     */
    @Transactional(readOnly = true)
    public WordCloudResponseDto generateWordCloud(Long questionId, int topN) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("질문을 찾을 수 없습니다: " + questionId));

        // 주관식 질문이 아닌 경우 예외 처리
        if (question.getType() != Question.QuestionType.SUBJECTIVE) {
            throw new IllegalArgumentException("주관식 질문만 워드클라우드를 생성할 수 있습니다: " + questionId);
        }

        // 1. 주관식 응답 수집
        List<Response> responses = responseRepository.findByQuestionId(questionId);
        List<String> subjectiveContents = responses.stream()
                .map(Response::getSubjectiveContent)
                .filter(Objects::nonNull)
                .filter(content -> !content.trim().isEmpty())
                .collect(Collectors.toList());

        // 전체 응답 수 (중복 제거된 세션 수)
        Long totalResponses = responseRepository.countDistinctResponseSessionsByQuestionId(questionId);

        // 응답이 없으면 빈 워드클라우드 반환 (200 OK로 응답)
        if (totalResponses == null || totalResponses == 0 || subjectiveContents.isEmpty()) {
            return new WordCloudResponseDto(questionId, Collections.emptyList(), 0L);
        }

        // 2. Gemini API 기반 키워드 추출 시도
        if (llmWordCloudEnabled) {
            try {
                List<WordCloudDto> llmKeywords = generateWordCloudWithGemini(
                        questionId,
                        question.getContent(),
                        subjectiveContents,
                        totalResponses,
                        topN
                );
                
                if (llmKeywords != null && !llmKeywords.isEmpty()) {
                    log.info("Gemini API 기반 워드클라우드 생성 성공: questionId={}, 키워드 수={}", 
                            questionId, llmKeywords.size());
                    return new WordCloudResponseDto(questionId, llmKeywords, totalResponses);
                }
            } catch (Exception e) {
                log.warn("Gemini API 기반 워드클라우드 생성 실패, Java 기반으로 폴백: questionId={}, reason={}", 
                        questionId, e.getMessage());
            }
        }

        // 3. 폴백: Java 기반 키워드 추출
        return generateWordCloudWithJava(questionId, subjectiveContents, totalResponses, topN);
    }

    /**
     * Gemini API를 사용하여 질문과 연관된 키워드를 추출합니다.
     *
     * @param questionId 질문 ID
     * @param questionContent 질문 내용
     * @param responses 주관식 응답 리스트
     * @param totalResponses 총 응답 수
     * @param topN 상위 N개 키워드
     * @return 키워드 리스트 (실패 시 null)
     */
    private List<WordCloudDto> generateWordCloudWithGemini(Long questionId,
                                                           String questionContent,
                                                           List<String> responses,
                                                           Long totalResponses,
                                                           int topN) {
        try {
            String prompt = buildWordCloudPrompt(questionContent, responses, totalResponses, topN);
            String raw = geminiApiService.generateText(prompt);
            return parseWordCloudKeywords(raw, topN);
        } catch (AiServiceException e) {
            log.warn("Gemini API 호출 실패: questionId={}, reason={}", questionId, e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("Gemini API 기반 키워드 추출 중 예외 발생: questionId={}", questionId, e);
            return null;
        }
    }

    /**
     * Gemini API용 프롬프트 생성
     */
    private String buildWordCloudPrompt(String questionContent,
                                        List<String> responses,
                                        Long totalResponses,
                                        int topN) {
        // 응답 샘플 (최대 30개)
        String samplesText;
        if (responses == null || responses.isEmpty()) {
            samplesText = "- 응답 샘플: 없음";
        } else {
            String joined = responses.stream()
                    .limit(30)
                    .map(s -> "• " + s)
                    .collect(Collectors.joining("\n"));
            samplesText = "- 응답 샘플:\n" + joined;
        }

        return String.format(
                "역할: 한국어 설문 분석가. 질문 내용과 응답을 분석하여 질문과 가장 연관된 키워드를 추출하라.\n" +
                        "\n" +
                        "규칙:\n" +
                        "- 질문의 의도와 목적을 고려하여 관련성 높은 키워드만 추출\n" +
                        "- 응답에서 실제로 언급된 단어/구문을 우선 추출\n" +
                        "- 일반적인 불필요한 단어(예: '입니다', '있습니다' 등) 제외\n" +
                        "- 키워드는 명사, 명사구 위주로 추출\n" +
                        "- 빈도수는 해당 키워드가 응답에서 언급된 횟수\n" +
                        "- 출력 형식: \"키워드1:빈도수, 키워드2:빈도수, ...\" (쉼표로 구분)\n" +
                        "- 최대 %d개의 키워드만 추출\n" +
                        "\n" +
                        "질문 내용:\n" +
                        "%s\n" +
                        "\n" +
                        "응답 정보:\n" +
                        "- 총 응답 수: %d\n" +
                        "%s\n" +
                        "\n" +
                        "위 정보를 바탕으로 질문과 가장 연관된 키워드를 추출하여 \"키워드:빈도수\" 형식으로 출력하라.",
                topN,
                questionContent != null ? questionContent : "질문 내용 없음",
                totalResponses != null ? totalResponses : 0,
                samplesText
        );
    }

    /**
     * Gemini 응답에서 키워드 파싱 (형식: "키워드1:빈도수, 키워드2:빈도수, ...")
     */
    private List<WordCloudDto> parseWordCloudKeywords(String raw, int maxCount) {
        if (raw == null || raw.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<WordCloudDto> keywords = new ArrayList<>();
        
        // "키워드:빈도수" 패턴 매칭
        Pattern pattern = Pattern.compile("([^:,]+):(\\d+)");
        Matcher matcher = pattern.matcher(raw);
        
        while (matcher.find() && keywords.size() < maxCount) {
            String word = matcher.group(1).trim();
            try {
                int count = Integer.parseInt(matcher.group(2).trim());
                if (!word.isEmpty() && count > 0) {
                    keywords.add(new WordCloudDto(word, count));
                }
            } catch (NumberFormatException e) {
                log.debug("빈도수 파싱 실패: {}", matcher.group(2));
            }
        }

        // 빈도수 기준 내림차순 정렬
        keywords.sort((a, b) -> b.getCount().compareTo(a.getCount()));

        return keywords;
    }

    /**
     * Java 기반 키워드 추출 (폴백)
     */
    private WordCloudResponseDto generateWordCloudWithJava(Long questionId,
                                                           List<String> subjectiveContents,
                                                           Long totalResponses,
                                                           int topN) {
        // 모든 응답을 하나의 텍스트로 합치기
        String combinedText = String.join(" ", subjectiveContents);

        // 키워드 추출 및 빈도수 계산
        List<String> keywords = textAnalysisService.extractKeywords(combinedText);

        // 상위 N개 키워드 추출
        List<Map.Entry<String, Integer>> topKeywords = textAnalysisService.getTopKeywords(keywords, topN);

        // DTO 변환
        List<WordCloudDto> wordCloud = topKeywords.stream()
                .map(entry -> new WordCloudDto(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        return new WordCloudResponseDto(questionId, wordCloud, totalResponses);
    }

    /**
     * 질문별 워드클라우드 생성 (기본값: 상위 50개)
     *
     * @param questionId 질문 ID
     * @return 워드클라우드 응답 DTO
     */
    @Transactional(readOnly = true)
    public WordCloudResponseDto generateWordCloud(Long questionId) {
        return generateWordCloud(questionId, 50);
    }

    /**
     * 질문별 워드클라우드 조회 (DB에서만 조회, 없으면 빈 데이터 반환)
     *
     * @param questionId 질문 ID
     * @return 워드클라우드 응답 DTO (DB에 없으면 빈 데이터 반환)
     */
    @Transactional(readOnly = true)
    public WordCloudResponseDto getWordCloud(Long questionId) {
        // DB에서 기존 워드클라우드 조회
        Optional<WordCloud> existingWordCloud = wordCloudRepository.findByQuestionId(questionId);
        
        if (existingWordCloud.isPresent()) {
            try {
                // DB에 저장된 워드클라우드가 있으면 역직렬화하여 반환
                String wordCloudData = existingWordCloud.get().getWordCloudData();
                if (wordCloudData != null && !wordCloudData.isEmpty()) {
                    return objectMapper.readValue(wordCloudData, WordCloudResponseDto.class);
                }
            } catch (JsonProcessingException e) {
                log.error("워드클라우드 역직렬화 실패: questionId={}", questionId, e);
            }
        }
        
        // DB에 없으면 빈 데이터 반환
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("질문을 찾을 수 없습니다: " + questionId));
        return new WordCloudResponseDto(questionId, Collections.emptyList(), 0L);
    }

    /**
     * 워드클라우드를 DB에 저장 (비동기 처리용)
     *
     * @param questionId 질문 ID
     */
    @Async("taskExecutor")
    @Transactional
    public void saveWordCloudAsync(Long questionId) {
        try {
            log.info("워드클라우드 생성 시작: questionId={}", questionId);
            
            // 워드클라우드 생성
            WordCloudResponseDto wordCloud = generateWordCloud(questionId);
            
            // JSON으로 직렬화
            String wordCloudData = objectMapper.writeValueAsString(wordCloud);
            
            // DB에 저장 또는 업데이트
            Optional<WordCloud> existing = wordCloudRepository.findByQuestionId(questionId);
            
            WordCloud wordCloudEntity;
            if (existing.isPresent()) {
                // 기존 워드클라우드 업데이트
                wordCloudEntity = existing.get();
                wordCloudEntity.setWordCloudData(wordCloudData);
            } else {
                // 새 워드클라우드 생성
                wordCloudEntity = WordCloud.builder()
                        .questionId(questionId)
                        .wordCloudData(wordCloudData)
                        .build();
            }
            
            wordCloudRepository.save(wordCloudEntity);
            log.info("워드클라우드 저장 완료: questionId={}", questionId);
            
        } catch (Exception e) {
            log.error("워드클라우드 저장 실패: questionId={}", questionId, e);
        }
    }

    /**
     * 설문의 모든 주관식 질문에 대해 워드클라우드 생성 및 저장
     *
     * @param surveyId 설문 ID
     */
    @Async("taskExecutor")
    @Transactional
    public void saveWordCloudsForSurveyAsync(Long surveyId) {
        try {
            log.info("설문의 모든 주관식 질문 워드클라우드 생성 시작: surveyId={}", surveyId);
            
            // 설문의 모든 질문 조회
            List<Question> questions = questionRepository.findBySurveyId(surveyId);
            
            // 주관식 질문만 필터링
            List<Question> subjectiveQuestions = questions.stream()
                    .filter(q -> q.getType() == Question.QuestionType.SUBJECTIVE)
                    .collect(Collectors.toList());
            
            // 각 주관식 질문에 대해 워드클라우드 생성 및 저장
            for (Question question : subjectiveQuestions) {
                saveWordCloudAsync(question.getId());
            }
            
            log.info("설문의 모든 주관식 질문 워드클라우드 생성 완료: surveyId={}, 질문 수={}", 
                    surveyId, subjectiveQuestions.size());
            
        } catch (Exception e) {
            log.error("설문 워드클라우드 저장 실패: surveyId={}", surveyId, e);
        }
    }
}

