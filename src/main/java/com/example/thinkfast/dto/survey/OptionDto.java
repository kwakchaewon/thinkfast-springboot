package com.example.thinkfast.dto.survey;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OptionDto {
    private Long id;
    private String content;

    public OptionDto(Long id, String content) {
        this.id = id;
        this.content = content;
    }
} 