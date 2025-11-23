package com.example.thinkfast.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeminiRequest {
    private List<Content> contents;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private List<Part> parts;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Part {
        private String text;
    }

    public static GeminiRequest create(String prompt) {
        Part part = new Part(prompt);
        Content content = new Content(List.of(part));
        return new GeminiRequest(List.of(content));
    }
}

