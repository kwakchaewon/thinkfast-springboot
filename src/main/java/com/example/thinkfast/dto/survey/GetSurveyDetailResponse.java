package com.example.thinkfast.dto.survey;

import java.time.LocalDateTime;

public interface GetSurveyDetailResponse {
    Long getId();
    Long getUserId();
    String getTitle();
    String getDescription();
    LocalDateTime getStartTime();
    LocalDateTime getEndTime();
    Boolean getIsActive();
    Boolean getShowResults();
    LocalDateTime getCreatedAt();
}
