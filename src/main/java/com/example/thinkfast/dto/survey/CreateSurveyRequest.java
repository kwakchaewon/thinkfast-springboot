package com.example.thinkfast.dto.survey;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
public class CreateSurveyRequest {
    private String title;
    private String description;
    private LocalDate endDate;
    private LocalTime endTime;
    private boolean showResults;
    private List<QuestionRequest> questions;

    @Getter
    @Setter
    public static class QuestionRequest {
        private String content;
        private String type; // MULTIPLE_CHOICE, SUBJECTIVE, SCALE
        private boolean required;
        private List<String> options; // 객관식 질문의 경우 옵션 목록
    }
}
