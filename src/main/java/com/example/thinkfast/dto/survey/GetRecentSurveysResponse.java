package com.example.thinkfast.dto.survey;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
public class GetRecentSurveysResponse {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private Boolean isActive;
    private LocalDate createdAt;
    private int responseCount;

    public GetRecentSurveysResponse(Long id, String title, String description, LocalDateTime startTime, Boolean isActive, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.isActive = isActive;
        this.createdAt = LocalDate.from(createdAt);

        // 고정값: 추후 응답 갯수 수정 예정
        this.responseCount = 5;
    }
}
