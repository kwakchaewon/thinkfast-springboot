package com.example.thinkfast.dto.survey;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
public class GetRecentSurveysResponse {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public GetRecentSurveysResponse(Long id, String title, String description, LocalDateTime startTime, Boolean isActive, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }
}
