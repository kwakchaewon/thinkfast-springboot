package com.example.thinkfast.service.ai;

import com.example.thinkfast.domain.survey.Question;
import com.example.thinkfast.dto.ai.OptionStatisticsDto;
import com.example.thinkfast.dto.ai.QuestionStatisticsDto;
import com.example.thinkfast.dto.ai.WordCloudDto;
import com.example.thinkfast.dto.ai.WordCloudResponseDto;
import com.example.thinkfast.repository.survey.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 인사이트 텍스트 생성 서비스
 * 통계 데이터를 기반으로 자연어 인사이트 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InsightService {

    private final SurveyStatisticsService statisticsService;
    private final WordCloudService wordCloudService;
    private final QuestionRepository questionRepository;

    /**
     * 객관식 질문 인사이트 생성
     *
     * @param questionId 질문 ID
     * @return 인사이트 텍스트
     */
    @Transactional(readOnly = true)
    public String generateMultipleChoiceInsight(Long questionId) {
        QuestionStatisticsDto statistics = statisticsService.getQuestionStatistics(questionId);

        if (statistics == null || statistics.getOptionStatistics() == null || statistics.getOptionStatistics().isEmpty()) {
            return "응답 데이터가 없어 인사이트를 생성할 수 없습니다.";
        }

        List<OptionStatisticsDto> options = statistics.getOptionStatistics();
        Long totalResponses = statistics.getTotalResponses();

        if (totalResponses == 0) {
            return "응답이 없어 인사이트를 생성할 수 없습니다.";
        }

        // 패턴 인식 및 템플릿 기반 문장 생성
        return generateMultipleChoiceInsightText(options, totalResponses);
    }

    /**
     * 객관식 인사이트 텍스트 생성 (패턴 인식 기반)
     *
     * @param options 옵션별 통계 리스트
     * @param totalResponses 전체 응답 수
     * @return 인사이트 텍스트
     */
    private String generateMultipleChoiceInsightText(List<OptionStatisticsDto> options, Long totalResponses) {
        // 옵션을 비율 내림차순으로 정렬
        List<OptionStatisticsDto> sortedOptions = options.stream()
                .sorted(Comparator.comparing(OptionStatisticsDto::getPercent).reversed())
                .collect(Collectors.toList());

        OptionStatisticsDto topOption = sortedOptions.get(0);
        Double topPercent = topOption.getPercent();

        // 패턴 1: 과반수 차지 (50% 이상)
        if (topPercent >= 50.0) {
            return String.format("%s이(가) 전체 응답의 %.1f%%를 차지하며 압도적으로 선호되고 있습니다.",
                    topOption.getOptionContent(), topPercent);
        }

        // 패턴 2: 상위 2개 옵션이 과반수 차지
        if (sortedOptions.size() >= 2) {
            OptionStatisticsDto secondOption = sortedOptions.get(1);
            Double combinedPercent = topPercent + secondOption.getPercent();
            if (combinedPercent >= 50.0) {
                return String.format("%s과(와) %s이(가) 응답자의 %.1f%%를 차지하며 주요 선택지로 나타났습니다.",
                        topOption.getOptionContent(), secondOption.getOptionContent(), combinedPercent);
            }
        }

        // 패턴 3: 상위 3개 옵션이 비슷한 비율 (차이가 10% 이내)
        if (sortedOptions.size() >= 3) {
            Double firstPercent = sortedOptions.get(0).getPercent();
            Double secondPercent = sortedOptions.get(1).getPercent();
            Double thirdPercent = sortedOptions.get(2).getPercent();

            if (Math.abs(firstPercent - secondPercent) <= 10.0 &&
                Math.abs(secondPercent - thirdPercent) <= 10.0) {
                return String.format("%s, %s, %s이(가) 비슷한 비율(각각 %.1f%%, %.1f%%, %.1f%%)로 나타났습니다.",
                        sortedOptions.get(0).getOptionContent(),
                        sortedOptions.get(1).getOptionContent(),
                        sortedOptions.get(2).getOptionContent(),
                        firstPercent, secondPercent, thirdPercent);
            }
        }

        // 패턴 4: 상위 옵션이 40% 이상
        if (topPercent >= 40.0) {
            return String.format("%s이(가) %.1f%%로 가장 높은 비율을 차지하고 있습니다.",
                    topOption.getOptionContent(), topPercent);
        }

        // 패턴 5: 일반적인 분포
        if (sortedOptions.size() >= 2) {
            return String.format("%s이(가) %.1f%%로 가장 높은 비율을 보였으며, %s이(가) %.1f%%로 그 뒤를 이었습니다.",
                    topOption.getOptionContent(), topPercent,
                    sortedOptions.get(1).getOptionContent(), sortedOptions.get(1).getPercent());
        }

        // 기본 템플릿
        return String.format("%s이(가) %.1f%%로 가장 높은 비율을 차지했습니다.",
                topOption.getOptionContent(), topPercent);
    }

    /**
     * 주관식 질문 인사이트 생성
     *
     * @param questionId 질문 ID
     * @return 인사이트 텍스트
     */
    @Transactional(readOnly = true)
    public String generateSubjectiveInsight(Long questionId) {
        WordCloudResponseDto wordCloud = wordCloudService.getWordCloud(questionId);

        if (wordCloud == null || wordCloud.getWordCloud() == null || wordCloud.getWordCloud().isEmpty()) {
            return "응답 데이터가 없어 인사이트를 생성할 수 없습니다.";
        }

        List<WordCloudDto> topWords = wordCloud.getWordCloud();
        Long totalResponses = wordCloud.getTotalResponses();

        if (totalResponses == 0) {
            return "응답이 없어 인사이트를 생성할 수 없습니다.";
        }

        // 주요 키워드 기반 문장 생성
        return generateSubjectiveInsightText(topWords, totalResponses);
    }

    /**
     * 주관식 인사이트 텍스트 생성 (키워드 기반)
     *
     * @param topWords 상위 키워드 리스트
     * @param totalResponses 전체 응답 수
     * @return 인사이트 텍스트
     */
    private String generateSubjectiveInsightText(List<WordCloudDto> topWords, Long totalResponses) {
        if (topWords.isEmpty()) {
            return "주요 키워드를 추출할 수 없습니다.";
        }

        // 상위 3개 키워드 추출
        List<String> topKeywords = topWords.stream()
                .limit(3)
                .map(WordCloudDto::getWord)
                .collect(Collectors.toList());

        WordCloudDto topWord = topWords.get(0);
        Integer topCount = topWord.getCount();

        // 패턴 1: 상위 키워드가 압도적으로 많음 (전체의 30% 이상)
        double topWordRatio = (topCount.doubleValue() / totalResponses.doubleValue()) * 100.0;
        if (topWordRatio >= 30.0) {
            return String.format("응답자들은 '%s'에 대한 언급이 가장 많았으며(총 %d회), 그 뒤를 '%s'과(와) '%s'이(가) 차지했습니다.",
                    topWord.getWord(), topCount,
                    topKeywords.size() > 1 ? topKeywords.get(1) : "",
                    topKeywords.size() > 2 ? topKeywords.get(2) : "");
        }

        // 패턴 2: 상위 3개 키워드가 비슷한 빈도
        if (topWords.size() >= 3) {
            Integer firstCount = topWords.get(0).getCount();
            Integer secondCount = topWords.get(1).getCount();
            Integer thirdCount = topWords.get(2).getCount();

            if (Math.abs(firstCount - secondCount) <= 2 && Math.abs(secondCount - thirdCount) <= 2) {
                return String.format("주요 키워드로는 '%s'(%d회), '%s'(%d회), '%s'(%d회)이(가) 자주 언급되었습니다.",
                        topKeywords.get(0), firstCount,
                        topKeywords.get(1), secondCount,
                        topKeywords.get(2), thirdCount);
            }
        }

        // 패턴 3: 일반적인 분포
        if (topKeywords.size() >= 2) {
            return String.format("응답자들은 '%s'에 대한 언급이 가장 많았으며(총 %d회), 그 뒤를 '%s'이(가) 차지했습니다.",
                    topWord.getWord(), topCount,
                    topKeywords.get(1));
        }

        // 기본 템플릿
        return String.format("주요 키워드로는 '%s'이(가) %d회 언급되었습니다.",
                topWord.getWord(), topCount);
    }

    /**
     * 질문별 인사이트 생성 (질문 타입에 따라 자동 선택)
     *
     * @param questionId 질문 ID
     * @return 인사이트 텍스트
     */
    @Transactional(readOnly = true)
    public String generateInsight(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("질문을 찾을 수 없습니다: " + questionId));

        if (question.getType() == Question.QuestionType.MULTIPLE_CHOICE) {
            return generateMultipleChoiceInsight(questionId);
        } else if (question.getType() == Question.QuestionType.SUBJECTIVE) {
            return generateSubjectiveInsight(questionId);
        } else {
            return "척도형 질문은 아직 인사이트를 제공하지 않습니다.";
        }
    }
}

