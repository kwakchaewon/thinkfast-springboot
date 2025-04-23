package com.example.thinkfast.dto.survey;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@Getter
public class GetSurveyDetailResponse {
    private Long id;
    private Long creatorId; // USER.id FK 대신 단순 ID로 처리
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean isActive;
    private LocalDate createdAt;
    private List<Question> questions;

    @NoArgsConstructor
    @Getter
    public static class Question {
        private Long id;
        private Long surveyId;
        private String questionType;
        private String content;
        private int orderIndex;
    }

    public GetSurveyDetailResponse(Long id, Long creatorId, String title, String description, LocalDateTime startTime, LocalDateTime endTime, Boolean isActive, LocalDateTime createdAt) {
        this.id = id;
        this.creatorId = creatorId;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isActive = isActive;
        this.createdAt = LocalDate.from(createdAt);
    }
}
