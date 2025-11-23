package com.example.thinkfast.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 객관식 옵션별 통계 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OptionStatisticsDto {
    private Long optionId;
    private String optionContent;
    private Long count;
    private Double percent;
}

