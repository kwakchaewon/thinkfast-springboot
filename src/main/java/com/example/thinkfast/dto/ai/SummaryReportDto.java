package com.example.thinkfast.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 요약 리포트 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SummaryReportDto {
    private String mainPosition; // 가장 많이 선택된 객관식 옵션
    private Double mainPositionPercent; // 해당 옵션의 비율
    private List<String> improvements; // 개선 사항 리스트
}

