package com.example.thinkfast.service.ai;

import com.example.thinkfast.domain.ai.WordCloud;
import com.example.thinkfast.domain.survey.Question;
import com.example.thinkfast.domain.survey.Response;
import com.example.thinkfast.dto.ai.WordCloudDto;
import com.example.thinkfast.dto.ai.WordCloudResponseDto;
import com.example.thinkfast.repository.ai.WordCloudRepository;
import com.example.thinkfast.repository.survey.QuestionRepository;
import com.example.thinkfast.repository.survey.ResponseRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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

    /**
     * 질문별 워드클라우드 생성
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

        if (subjectiveContents.isEmpty()) {
            return new WordCloudResponseDto(questionId, Collections.emptyList(), totalResponses);
        }

        // 2. 모든 응답을 하나의 텍스트로 합치기
        String combinedText = String.join(" ", subjectiveContents);

        // 3. 키워드 추출 및 빈도수 계산
        List<String> keywords = textAnalysisService.extractKeywords(combinedText);

        // 4. 상위 N개 키워드 추출
        List<Map.Entry<String, Integer>> topKeywords = textAnalysisService.getTopKeywords(keywords, topN);

        // 5. DTO 변환
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
     * 질문별 워드클라우드 조회 (DB에서 먼저 조회, 없으면 생성)
     *
     * @param questionId 질문 ID
     * @return 워드클라우드 응답 DTO
     */
    @Transactional(readOnly = true)
    public WordCloudResponseDto getWordCloud(Long questionId) {
        // 1. DB에서 기존 워드클라우드 조회
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
        
        // 2. DB에 없으면 새로 생성 (실시간 계산)
        return generateWordCloud(questionId);
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

