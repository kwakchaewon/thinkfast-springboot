package com.example.thinkfast.dto.survey;

import java.time.LocalDateTime;

public interface GetSurveyDetailResponse {
    Long getId();
    Long getCreatorId();
    String getTitle();
    String getDescription();
    LocalDateTime getStartTime();
    LocalDateTime getEndTime();
    Boolean getIsActive();
    LocalDateTime getCreatedAt();
}
