package com.example.thinkfast.service.ai;

import com.example.thinkfast.domain.survey.Question;
import com.example.thinkfast.domain.survey.Response;
import com.example.thinkfast.repository.survey.QuestionRepository;
import com.example.thinkfast.repository.survey.ResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 개선 사항 추출 서비스
 * 주관식 질문에서 개선 관련 키워드를 추출하고 템플릿 기반 문장을 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImprovementExtractionService {

    private final ResponseRepository responseRepository;
    private final QuestionRepository questionRepository;
    private final TextAnalysisService textAnalysisService;

    // 개선 관련 키워드 사전 (하드코딩)
    private static final Set<String> IMPROVEMENT_KEYWORDS = new HashSet<>(Arrays.asList(
            "개선", "향상", "보완", "수정", "변경", "개발", "추가", "보강",
            "필요", "요청", "제안", "건의", "바람", "원함", "희망",
            "문제", "이슈", "버그", "오류", "에러", "장애", "불편",
            "최적화", "성능", "속도", "안정성", "안정화", "보안",
            "매칭", "시스템", "서버", "클라이언트", "인터페이스", "UI", "UX",
            "패치", "업데이트", "업그레이드", "리뉴얼", "개편"
    ));

    /**
     * 주관식 질문에서 개선 관련 키워드 추출
     *
     * @param questionId 질문 ID
     * @return 개선 관련 키워드와 빈도수 리스트
     */
    @Transactional(readOnly = true)
    public List<Map.Entry<String, Integer>> extractImprovementKeywords(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("질문을 찾을 수 없습니다: " + questionId));

        // 주관식 질문이 아닌 경우 예외 처리
        if (question.getType() != Question.QuestionType.SUBJECTIVE) {
            throw new IllegalArgumentException("주관식 질문만 개선 사항을 추출할 수 있습니다: " + questionId);
        }

        // 주관식 응답 수집
        List<Response> responses = responseRepository.findByQuestionId(questionId);
        List<String> subjectiveContents = responses.stream()
                .map(Response::getSubjectiveContent)
                .filter(Objects::nonNull)
                .filter(content -> !content.trim().isEmpty())
                .collect(Collectors.toList());

        if (subjectiveContents.isEmpty()) {
            return Collections.emptyList();
        }

        // 모든 응답을 하나의 텍스트로 합치기
        String combinedText = String.join(" ", subjectiveContents);

        // 키워드 추출
        List<String> keywords = textAnalysisService.extractKeywords(combinedText);

        // 개선 관련 키워드만 필터링
        List<String> improvementKeywords = keywords.stream()
                .filter(keyword -> IMPROVEMENT_KEYWORDS.contains(keyword) || 
                        keyword.contains("개선") || keyword.contains("향상") || 
                        keyword.contains("수정") || keyword.contains("필요"))
                .collect(Collectors.toList());

        // 빈도수 계산 및 정렬
        return textAnalysisService.getTopKeywords(improvementKeywords, 20);
    }

    /**
     * 설문의 모든 주관식 질문에서 개선 사항 추출
     *
     * @param surveyId 설문 ID
     * @return 개선 관련 키워드와 빈도수 리스트 (모든 주관식 질문 통합)
     */
    @Transactional(readOnly = true)
    public List<Map.Entry<String, Integer>> extractImprovementKeywordsFromSurvey(Long surveyId) {
        // 설문의 모든 질문 조회
        List<Question> questions = questionRepository.findBySurveyId(surveyId);

        // 주관식 질문만 필터링
        List<Question> subjectiveQuestions = questions.stream()
                .filter(q -> q.getType() == Question.QuestionType.SUBJECTIVE)
                .collect(Collectors.toList());

        if (subjectiveQuestions.isEmpty()) {
            return Collections.emptyList();
        }

        // 모든 주관식 응답 수집
        List<String> allSubjectiveContents = new ArrayList<>();
        for (Question question : subjectiveQuestions) {
            List<Response> responses = responseRepository.findByQuestionId(question.getId());
            List<String> contents = responses.stream()
                    .map(Response::getSubjectiveContent)
                    .filter(Objects::nonNull)
                    .filter(content -> !content.trim().isEmpty())
                    .collect(Collectors.toList());
            allSubjectiveContents.addAll(contents);
        }

        if (allSubjectiveContents.isEmpty()) {
            return Collections.emptyList();
        }

        // 모든 응답을 하나의 텍스트로 합치기
        String combinedText = String.join(" ", allSubjectiveContents);

        // 키워드 추출
        List<String> keywords = textAnalysisService.extractKeywords(combinedText);

        // 개선 관련 키워드만 필터링
        List<String> improvementKeywords = keywords.stream()
                .filter(keyword -> IMPROVEMENT_KEYWORDS.contains(keyword) || 
                        keyword.contains("개선") || keyword.contains("향상") || 
                        keyword.contains("수정") || keyword.contains("필요"))
                .collect(Collectors.toList());

        // 빈도수 계산 및 정렬
        return textAnalysisService.getTopKeywords(improvementKeywords, 20);
    }

    /**
     * 템플릿 기반 개선 사항 문장 생성
     *
     * @param keywords 개선 관련 키워드와 빈도수 리스트
     * @param maxCount 최대 생성할 문장 수
     * @return 개선 사항 문장 리스트
     */
    public List<String> generateImprovementSentences(List<Map.Entry<String, Integer>> keywords, int maxCount) {
        if (keywords == null || keywords.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> sentences = new ArrayList<>();
        int count = 0;

        for (Map.Entry<String, Integer> entry : keywords) {
            if (count >= maxCount) {
                break;
            }

            String keyword = entry.getKey();
            Integer frequency = entry.getValue();

            // 템플릿 기반 문장 생성
            String sentence = generateSentenceFromKeyword(keyword, frequency);
            if (sentence != null && !sentence.isEmpty()) {
                sentences.add(sentence);
                count++;
            }
        }

        return sentences;
    }

    /**
     * 키워드와 빈도수를 기반으로 문장 생성
     *
     * @param keyword 키워드
     * @param frequency 빈도수
     * @return 생성된 문장
     */
    private String generateSentenceFromKeyword(String keyword, Integer frequency) {
        // 빈도수가 2 이상인 키워드만 문장 생성
        if (frequency < 2) {
            return null;
        }

        // 템플릿 리스트
        List<String> templates = Arrays.asList(
                keyword + "에 대한 개선이 필요합니다",
                keyword + " 관련 기능의 향상이 요청되었습니다",
                keyword + " 부분의 수정이 필요합니다",
                keyword + "에 대한 보완이 요청되었습니다",
                keyword + " 관련 개선 사항이 제안되었습니다"
        );

        // 빈도수에 따라 템플릿 선택 (빈도수가 높을수록 더 강한 표현)
        int templateIndex = Math.min(frequency - 2, templates.size() - 1);
        return templates.get(templateIndex);
    }

    /**
     * 설문의 개선 사항 추출 (전체 프로세스)
     *
     * @param surveyId 설문 ID
     * @param maxCount 최대 생성할 개선 사항 수
     * @return 개선 사항 문장 리스트
     */
    @Transactional(readOnly = true)
    public List<String> extractImprovements(Long surveyId, int maxCount) {
        // 개선 관련 키워드 추출
        List<Map.Entry<String, Integer>> keywords = extractImprovementKeywordsFromSurvey(surveyId);

        // 템플릿 기반 문장 생성
        return generateImprovementSentences(keywords, maxCount);
    }
}

