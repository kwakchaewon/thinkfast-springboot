package com.example.thinkfast.dto.survey;

import com.example.thinkfast.domain.survey.Question;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class QuestionDto {
    private Long id;
    private Long surveyId;
    private Question.QuestionType type;
    private String content;
    private int orderIndex;
    private List<OptionDto> options;

    public QuestionDto(Long id, Long surveyId, Question.QuestionType type, String content, int orderIndex) {
        this.id = id;
        this.surveyId = surveyId;
        this.type = type;
        this.content = content;
        this.orderIndex = orderIndex;
    }
}
