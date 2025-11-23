package com.example.thinkfast.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 질문별 통계 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionStatisticsDto {
    private Long questionId;
    private String questionType;
    private String questionContent;
    private Long totalResponses;
    private List<OptionStatisticsDto> optionStatistics;
    private OptionStatisticsDto topOption; // 비율이 가장 높은 옵션
}

