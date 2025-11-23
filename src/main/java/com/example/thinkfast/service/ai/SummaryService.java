package com.example.thinkfast.service.ai;

import com.example.thinkfast.dto.ai.OptionStatisticsDto;
import com.example.thinkfast.dto.ai.SummaryReportDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 요약 리포트 생성 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryService {

    private final SurveyStatisticsService statisticsService;
    private final ImprovementExtractionService improvementExtractionService;

    /**
     * 설문 요약 리포트 생성
     *
     * @param surveyId 설문 ID
     * @return 요약 리포트 DTO
     */
    @Transactional(readOnly = true)
    public SummaryReportDto generateSummaryReport(Long surveyId) {
        // 1. 첫 번째 객관식 질문에서 비율이 가장 높은 옵션 추출 (mainPosition)
        OptionStatisticsDto topOption = statisticsService.getFirstQuestionTopOption(surveyId);

        String mainPosition = null;
        Double mainPositionPercent = null;

        if (topOption != null) {
            mainPosition = topOption.getOptionContent();
            mainPositionPercent = topOption.getPercent();
        }

        // 2. 주관식 질문에서 개선 사항 추출
        List<String> improvements = improvementExtractionService.extractImprovements(surveyId, 5);

        return new SummaryReportDto(mainPosition, mainPositionPercent, improvements);
    }

    /**
     * 설문 요약 리포트 생성 (옵션 지정)
     *
     * @param surveyId 설문 ID
     * @param maxImprovements 최대 개선 사항 수
     * @return 요약 리포트 DTO
     */
    @Transactional(readOnly = true)
    public SummaryReportDto generateSummaryReport(Long surveyId, int maxImprovements) {
        // 1. 첫 번째 객관식 질문에서 비율이 가장 높은 옵션 추출 (mainPosition)
        OptionStatisticsDto topOption = statisticsService.getFirstQuestionTopOption(surveyId);

        String mainPosition = null;
        Double mainPositionPercent = null;

        if (topOption != null) {
            mainPosition = topOption.getOptionContent();
            mainPositionPercent = topOption.getPercent();
        }

        // 2. 주관식 질문에서 개선 사항 추출
        List<String> improvements = improvementExtractionService.extractImprovements(surveyId, maxImprovements);

        return new SummaryReportDto(mainPosition, mainPositionPercent, improvements);
    }
}

