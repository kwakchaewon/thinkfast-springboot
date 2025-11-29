package com.example.thinkfast.dto.survey;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PublicSurveyListResponse {
    private List<PublicSurveyDto> surveys;
    private PaginationDto pagination;
}


