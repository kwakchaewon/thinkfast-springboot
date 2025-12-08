package com.example.thinkfast.service.ai;

import com.example.thinkfast.domain.ai.InsightReport;
import com.example.thinkfast.domain.survey.Question;
import com.example.thinkfast.domain.survey.Response;
import com.example.thinkfast.dto.ai.OptionStatisticsDto;
import com.example.thinkfast.dto.ai.SummaryReportDto;
import com.example.thinkfast.repository.ai.InsightReportRepository;
import com.example.thinkfast.repository.survey.QuestionRepository;
import com.example.thinkfast.repository.survey.ResponseRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 요약 리포트 생성 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryService {

    private final SurveyStatisticsService statisticsService;
    private final ImprovementExtractionService improvementExtractionService;
    private final LlmSummaryGenerator llmSummaryGenerator;
    private final InsightReportRepository insightReportRepository;
    private final ResponseRepository responseRepository;
    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;

    /**
     * 설문 요약 리포트 조회 (DB에서만 조회, 없으면 빈 데이터 반환)
     *
     * @param surveyId 설문 ID
     * @return 요약 리포트 DTO (DB에 없으면 빈 데이터 반환)
     */
    @Transactional(readOnly = true)
    public SummaryReportDto getSummaryReport(Long surveyId) {
        // DB에서 기존 리포트 조회
        Optional<InsightReport> existingReport = insightReportRepository.findBySurveyId(surveyId);
        
        if (existingReport.isPresent()) {
            try {
                // DB에 저장된 리포트가 있으면 역직렬화하여 반환
                String summaryText = existingReport.get().getSummaryText();
                if (summaryText != null && !summaryText.isEmpty()) {
                    return objectMapper.readValue(summaryText, SummaryReportDto.class);
                }
            } catch (JsonProcessingException e) {
                log.error("요약 리포트 역직렬화 실패: surveyId={}", surveyId, e);
            }
        }
        
        // DB에 없으면 빈 데이터 반환
        return new SummaryReportDto(null, null, new ArrayList<>());
    }

    /**
     * 설문 요약 리포트 생성 (실시간 계산)
     *
     * @param surveyId 설문 ID
     * @return 요약 리포트 DTO (응답이 없으면 빈 데이터 반환)
     */
    @Transactional(readOnly = true)
    public SummaryReportDto generateSummaryReport(Long surveyId) {
        // 0. 설문에 응답이 있는지 확인
        Long responseCount = responseRepository.countDistinctResponseSessionsBySurveyId(surveyId);
        if (responseCount == null || responseCount == 0) {
            // 응답이 없으면 빈 데이터 반환 (200 OK로 응답)
            return new SummaryReportDto(null, null, new ArrayList<>());
        }

        // 1. 첫 번째 객관식 질문에서 비율이 가장 높은 옵션 추출 (mainPosition)
        OptionStatisticsDto topOption = statisticsService.getFirstQuestionTopOption(surveyId);

        String mainPosition = null;
        Double mainPositionPercent = null;

        if (topOption != null) {
            mainPosition = topOption.getOptionContent();
            mainPositionPercent = topOption.getPercent();
        }

        // 2. LLM 기반 개선/인사이트 생성 시도 (폴백 포함)
        List<String> improvements = generateImprovementsWithFallback(surveyId, responseCount, topOption, 5);

        return new SummaryReportDto(mainPosition, mainPositionPercent, improvements);
    }

    /**
     * 설문 요약 리포트 생성 (옵션 지정)
     *
     * @param surveyId 설문 ID
     * @param maxImprovements 최대 개선 사항 수
     * @return 요약 리포트 DTO
     */
    @Deprecated
    @Transactional(readOnly = true)
    public SummaryReportDto generateSummaryReport(Long surveyId, int maxImprovements) {
        // 0. 설문에 응답이 있는지 확인
        Long responseCount = responseRepository.countDistinctResponseSessionsBySurveyId(surveyId);
        if (responseCount == null || responseCount == 0) {
            // 응답이 없으면 빈 데이터 반환 (200 OK로 응답)
            return new SummaryReportDto(null, null, new ArrayList<>());
        }

        // 1. 첫 번째 객관식 질문에서 비율이 가장 높은 옵션 추출 (mainPosition)
        OptionStatisticsDto topOption = statisticsService.getFirstQuestionTopOption(surveyId);

        String mainPosition = null;
        Double mainPositionPercent = null;

        if (topOption != null) {
            mainPosition = topOption.getOptionContent();
            mainPositionPercent = topOption.getPercent();
        }

        // 2. LLM 기반 개선/인사이트 생성 시도 (폴백 포함)
        List<String> improvements = generateImprovementsWithFallback(surveyId, responseCount, topOption, maxImprovements);

        return new SummaryReportDto(mainPosition, mainPositionPercent, improvements);
    }

    /**
     * 요약 리포트를 DB에 저장 (비동기 처리용)
     *
     * @param surveyId 설문 ID
     */
    @Async("taskExecutor")
    @Transactional
    public void saveSummaryReportAsync(Long surveyId) {
        try {
            log.info("요약 리포트 생성 시작: surveyId={}", surveyId);
            
            // 리포트 생성
            SummaryReportDto summary = generateSummaryReport(surveyId);
            
            // JSON으로 직렬화
            String summaryText = objectMapper.writeValueAsString(summary);
            
            // 개선 사항 키워드 추출 (키워드만)
            List<Map.Entry<String, Integer>> keywords = 
                improvementExtractionService.extractImprovementKeywordsFromSurvey(surveyId);
            String keywordsJson = objectMapper.writeValueAsString(keywords);
            
            // DB에 저장 또는 업데이트
            Optional<InsightReport> existing = insightReportRepository.findBySurveyId(surveyId);
            
            InsightReport report;
            if (existing.isPresent()) {
                // 기존 리포트 업데이트
                report = existing.get();
                report.setSummaryText(summaryText);
                report.setKeywords(keywordsJson);
            } else {
                // 새 리포트 생성
                report = InsightReport.builder()
                        .surveyId(surveyId)
                        .summaryText(summaryText)
                        .keywords(keywordsJson)
                        .build();
            }
            
            insightReportRepository.save(report);
            log.info("요약 리포트 저장 완료: surveyId={}", surveyId);
            
        } catch (Exception e) {
            log.error("요약 리포트 저장 실패: surveyId={}", surveyId, e);
        }
    }

    /**
     * LLM 기반 개선/인사이트 생성 후 실패 시 템플릿 기반으로 폴백.
     */
    private List<String> generateImprovementsWithFallback(Long surveyId,
                                                          Long totalResponses,
                                                          OptionStatisticsDto topOption,
                                                          int maxCount) {
        // 1) LLM 시도
        List<Map.Entry<String, Integer>> keywordEntries = improvementExtractionService
                .extractImprovementKeywordsFromSurvey(surveyId);
        List<String> topKeywords = keywordEntries.stream()
                .map(Map.Entry::getKey)
                .limit(10)
                .collect(Collectors.toList());
        List<String> sampleResponses = collectSubjectiveSamples(surveyId, 20);

        List<String> llmResults = llmSummaryGenerator.generateInsights(
                surveyId,
                totalResponses,
                topOption,
                topKeywords,
                sampleResponses,
                maxCount
        );

        if (llmResults != null && !llmResults.isEmpty()) {
            return llmResults;
        }

        // 2) 폴백: 기존 템플릿 기반 개선 문장
        return improvementExtractionService.extractImprovements(surveyId, maxCount);
    }

    /**
     * 설문 내 주관식 응답 샘플을 수집한다.
     */
    private List<String> collectSubjectiveSamples(Long surveyId, int limit) {
        try {
            List<Question> questions = questionRepository.findBySurveyId(surveyId);
            if (questions.isEmpty()) {
                return new ArrayList<>();
            }

            // 주관식 질문 ID 집합
            List<Long> subjectiveQuestionIds = questions.stream()
                    .filter(q -> q.getType() == Question.QuestionType.SUBJECTIVE)
                    .map(Question::getId)
                    .collect(Collectors.toList());

            if (subjectiveQuestionIds.isEmpty()) {
                return new ArrayList<>();
            }

            // 설문 전체 응답에서 주관식만 필터링
            List<Response> responses = responseRepository.findBySurveyId(surveyId);
            return responses.stream()
                    .filter(r -> r.getQuestionId() != null && subjectiveQuestionIds.contains(r.getQuestionId()))
                    .map(Response::getSubjectiveContent)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .limit(limit)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.debug("주관식 응답 샘플 수집 중 예외: surveyId={}", surveyId, e);
            return new ArrayList<>();
        }
    }
}

