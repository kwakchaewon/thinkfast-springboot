package com.example.thinkfast.dto.survey;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class GetRecentSurveysResponse {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
