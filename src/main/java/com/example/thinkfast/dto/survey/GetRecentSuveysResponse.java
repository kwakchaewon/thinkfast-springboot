package com.example.thinkfast.dto.survey;

import java.time.LocalDateTime;

public class GetRecentSuveysResponse {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private int answerCnt = 5;
}
