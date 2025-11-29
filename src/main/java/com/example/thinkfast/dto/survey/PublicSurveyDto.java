package com.example.thinkfast.dto.survey;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PublicSurveyDto {
    private Long id;
    private String title;
    private String description;
    private Boolean isActive;
    private LocalDateTime endTime;
    private Long responseCount;
    private LocalDateTime createdAt;
    private Boolean showResults;
    private Long ownerId;
    private String ownerName;
}


