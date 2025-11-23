package com.example.thinkfast.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 워드클라우드 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WordCloudResponseDto {
    private Long questionId;
    private List<WordCloudDto> wordCloud;
    private Long totalResponses;
}

