package com.example.thinkfast.service.ai;

import com.example.thinkfast.domain.survey.Option;
import com.example.thinkfast.domain.survey.Question;
import com.example.thinkfast.domain.survey.Response;
import com.example.thinkfast.dto.ai.OptionStatisticsDto;
import com.example.thinkfast.dto.ai.QuestionStatisticsDto;
import com.example.thinkfast.dto.ai.QuestionStatisticsResponseDto;
import com.example.thinkfast.repository.survey.OptionRepository;
import com.example.thinkfast.repository.survey.QuestionRepository;
import com.example.thinkfast.repository.survey.ResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 설문 통계 집계 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SurveyStatisticsService {

    private final ResponseRepository responseRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;

    /**
     * 객관식 질문별 주요 응답 옵션 추출 및 통계 집계
     *
     * @param questionId 질문 ID
     * @return 질문별 통계 DTO
     */
    @Transactional(readOnly = true)
    public QuestionStatisticsDto getQuestionStatistics(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("질문을 찾을 수 없습니다: " + questionId));

        // 질문 타입이 객관식이 아닌 경우 예외 처리
        if (question.getType() != Question.QuestionType.MULTIPLE_CHOICE) {
            throw new IllegalArgumentException("객관식 질문만 통계를 제공할 수 있습니다: " + questionId);
        }

        // 전체 응답 수 조회 (중복 제거된 세션 수)
        Long totalResponses = responseRepository.countDistinctResponseSessionsByQuestionId(questionId);

        // 옵션별 응답 수 집계
        List<Object[]> optionCounts = responseRepository.countByQuestionIdAndOptionId(questionId);

        // 옵션 정보 조회
        List<Option> options = optionRepository.findByQuestionIdOrderByIdAsc(questionId);

        // 옵션별 통계 생성
        List<OptionStatisticsDto> optionStatistics = new ArrayList<>();
        Map<Long, Long> optionCountMap = optionCounts.stream()
                .collect(Collectors.toMap(
                        arr -> (Long) arr[0],
                        arr -> (Long) arr[1]
                ));

        for (Option option : options) {
            Long count = optionCountMap.getOrDefault(option.getId(), 0L);
            Double percent = totalResponses > 0 
                    ? (count.doubleValue() / totalResponses.doubleValue()) * 100.0 
                    : 0.0;

            optionStatistics.add(new OptionStatisticsDto(
                    option.getId(),
                    option.getContent(),
                    count,
                    Math.round(percent * 100.0) / 100.0 // 소수점 둘째 자리까지
            ));
        }

        // 비율이 가장 높은 옵션 식별
        OptionStatisticsDto topOption = optionStatistics.stream()
                .max(Comparator.comparing(OptionStatisticsDto::getCount))
                .orElse(null);

        return new QuestionStatisticsDto(
                questionId,
                question.getType().toString(),
                question.getContent(),
                totalResponses,
                optionStatistics,
                topOption
        );
    }

    /**
     * 설문의 모든 객관식 질문별 통계 집계
     *
     * @param surveyId 설문 ID
     * @return 질문별 통계 리스트
     */
    @Transactional(readOnly = true)
    public List<QuestionStatisticsDto> getSurveyStatistics(Long surveyId) {
        // 설문의 모든 질문 조회
        List<Question> questions = questionRepository.findBySurveyId(surveyId);

        // 객관식 질문만 필터링
        List<Question> multipleChoiceQuestions = questions.stream()
                .filter(q -> q.getType() == Question.QuestionType.MULTIPLE_CHOICE)
                .sorted(Comparator.comparing(Question::getOrderIndex))
                .collect(Collectors.toList());

        // 각 질문별 통계 집계
        return multipleChoiceQuestions.stream()
                .map(question -> getQuestionStatistics(question.getId()))
                .collect(Collectors.toList());
    }

    /**
     * 비율이 가장 높은 옵션 식별 (질문 ID로)
     *
     * @param questionId 질문 ID
     * @return 비율이 가장 높은 옵션 통계
     */
    @Transactional(readOnly = true)
    public OptionStatisticsDto getTopOption(Long questionId) {
        QuestionStatisticsDto statistics = getQuestionStatistics(questionId);
        return statistics.getTopOption();
    }

    /**
     * 설문의 첫 번째 객관식 질문에서 비율이 가장 높은 옵션 추출
     * (요약 리포트의 mainPosition 등에 사용)
     *
     * @param surveyId 설문 ID
     * @return 비율이 가장 높은 옵션 통계
     */
    @Transactional(readOnly = true)
    public OptionStatisticsDto getFirstQuestionTopOption(Long surveyId) {
        List<Question> questions = questionRepository.findBySurveyId(surveyId);

        Optional<Question> firstMultipleChoiceQuestion = questions.stream()
                .filter(q -> q.getType() == Question.QuestionType.MULTIPLE_CHOICE)
                .min(Comparator.comparing(Question::getOrderIndex));

        if (firstMultipleChoiceQuestion.isPresent()) {
            return getTopOption(firstMultipleChoiceQuestion.get().getId());
        }

        return null;
    }

    /**
     * 주관식 질문별 통계 조회
     *
     * @param questionId 질문 ID
     * @return 전체 응답 수
     */
    @Transactional(readOnly = true)
    public Long getSubjectiveQuestionStatistics(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("질문을 찾을 수 없습니다: " + questionId));

        // 질문 타입이 주관식이 아닌 경우 예외 처리
        if (question.getType() != Question.QuestionType.SUBJECTIVE) {
            throw new IllegalArgumentException("주관식 질문만 통계를 제공할 수 있습니다: " + questionId);
        }

        // 전체 응답 수 조회 (중복 제거된 세션 수)
        return responseRepository.countDistinctResponseSessionsByQuestionId(questionId);
    }

    /**
     * 질문별 통계 조회 (객관식/주관식 모두 지원)
     * API 응답 형식으로 반환
     *
     * @param questionId 질문 ID
     * @return 질문별 통계 응답 DTO
     */
    @Transactional(readOnly = true)
    public QuestionStatisticsResponseDto getQuestionStatisticsResponse(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("질문을 찾을 수 없습니다: " + questionId));

        QuestionStatisticsResponseDto response = new QuestionStatisticsResponseDto();
        response.setQuestionId(questionId);
        response.setType(question.getType().toString());

        QuestionStatisticsResponseDto.Statistics statistics = new QuestionStatisticsResponseDto.Statistics();

        if (question.getType() == Question.QuestionType.MULTIPLE_CHOICE) {
            // 객관식 질문 통계
            Long totalResponses = responseRepository.countDistinctResponseSessionsByQuestionId(questionId);

            // 옵션별 응답 수 집계
            List<Object[]> optionCounts = responseRepository.countByQuestionIdAndOptionId(questionId);

            // 옵션 정보 조회
            List<Option> options = optionRepository.findByQuestionIdOrderByIdAsc(questionId);

            // 옵션별 통계 생성
            List<QuestionStatisticsResponseDto.OptionStatistics> optionStatistics = new ArrayList<>();
            Map<Long, Long> optionCountMap = optionCounts.stream()
                    .collect(Collectors.toMap(
                            arr -> (Long) arr[0],
                            arr -> (Long) arr[1]
                    ));

            for (Option option : options) {
                Long count = optionCountMap.getOrDefault(option.getId(), 0L);
                Double percent = totalResponses > 0
                        ? (count.doubleValue() / totalResponses.doubleValue()) * 100.0
                        : 0.0;

                // 소수점 첫째 자리까지 반올림
                Double roundedPercent = Math.round(percent * 10.0) / 10.0;

                QuestionStatisticsResponseDto.OptionStatistics optionStat = 
                        new QuestionStatisticsResponseDto.OptionStatistics(
                                option.getId(),
                                option.getContent(),
                                count,
                                roundedPercent
                        );
                optionStatistics.add(optionStat);
            }

            statistics.setOptions(optionStatistics);
            statistics.setTotalResponses(totalResponses);

        } else if (question.getType() == Question.QuestionType.SUBJECTIVE) {
            // 주관식 질문 통계 (전체 응답 수만)
            Long totalResponses = responseRepository.countDistinctResponseSessionsByQuestionId(questionId);
            statistics.setTotalResponses(totalResponses);
            // options 필드는 null 또는 빈 리스트로 설정되지 않음 (필드 자체를 포함하지 않음)
            statistics.setOptions(null);
        } else {
            // 척도형 질문은 현재 지원하지 않음
            throw new IllegalArgumentException("척도형 질문은 아직 통계를 제공하지 않습니다: " + questionId);
        }

        response.setStatistics(statistics);
        return response;
    }
}

