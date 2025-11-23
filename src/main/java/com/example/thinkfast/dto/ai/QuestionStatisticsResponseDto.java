package com.example.thinkfast.dto.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 질문별 통계 API 응답 DTO
 * API 명세서에 맞춘 응답 형식
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuestionStatisticsResponseDto {
    private Long questionId;
    private String type; // MULTIPLE_CHOICE, SUBJECTIVE, SCALE
    private Statistics statistics;
    private String insight; // 선택적, 없을 수 있음

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Statistics {
        private List<OptionStatistics> options; // 객관식 질문인 경우만 포함
        private Long totalResponses;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionStatistics {
        private Long optionId;
        private String optionContent;
        private Long count;
        private Double percent;
    }
}

