package com.example.thinkfast.dto.survey;

import com.example.thinkfast.domain.survey.Question;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponsesResponseDto {
    private Long questionId;
    private Question.QuestionType type;
    private List<ResponseItemDto> responses;
    private PaginationDto pagination;
}

