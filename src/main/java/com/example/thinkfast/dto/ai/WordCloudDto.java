package com.example.thinkfast.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 워드클라우드 단어 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WordCloudDto {
    private String word; // 키워드
    private Integer count; // 빈도수
}

