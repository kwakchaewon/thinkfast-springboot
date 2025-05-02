package com.example.thinkfast.dto.survey;

import com.example.thinkfast.domain.survey.QuestionType;
import lombok.Getter;

import java.util.List;

@Getter
public class CreateResponseRequest {
    List<CreateResponseDto> answers;

    @Getter
    public static class CreateResponseDto{
        private Long questionId; // questionId
        private QuestionType type;
        private Long optionId; // 객관식 선택지 (nullable)
        private String content; // 주관식 선택지 (nullable)
    }
}
