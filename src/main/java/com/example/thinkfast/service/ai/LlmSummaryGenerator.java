package com.example.thinkfast.service.ai;

import com.example.thinkfast.dto.ai.OptionStatisticsDto;
import com.example.thinkfast.exception.AiServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * LLM을 사용해 설문 요약용 자연어 인사이트를 생성하는 헬퍼.
 * - Gemini 호출
 * - 실패 시 호출부에서 템플릿 기반 로직으로 폴백
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmSummaryGenerator {

    private final GeminiApiService geminiApiService;

    @Value("${llm.summary.enabled:true}")
    private boolean llmSummaryEnabled;

    /**
     * 설문 응답을 기반으로 자연어 개선/인사이트 문장을 생성한다.
     *
     * @param surveyId          설문 ID (로그 추적용)
     * @param totalResponses    총 응답 세션 수
     * @param topOption         첫 객관식 질문의 최다 응답 옵션
     * @param topKeywords       주관식 키워드 상위 N개
     * @param sampleResponses   주관식 응답 샘플 (최대 20개 정도)
     * @param maxCount          생성할 문장 수 (최대 5 권장)
     * @return LLM이 생성한 문장 리스트 (빈 리스트 가능)
     */
    public List<String> generateInsights(Long surveyId,
                                         Long totalResponses,
                                         OptionStatisticsDto topOption,
                                         List<String> topKeywords,
                                         List<String> sampleResponses,
                                         int maxCount) {
        if (!llmSummaryEnabled) {
            log.debug("LLM 요약 비활성화 - 설정값 llm.summary.enabled=false");
            return new ArrayList<>();
        }

        try {
            String prompt = buildPrompt(totalResponses, topOption, topKeywords, sampleResponses, maxCount);
            String raw = geminiApiService.generateText(prompt);
            return parseBullets(raw, maxCount);
        } catch (AiServiceException e) {
            log.warn("LLM 요약 생성 실패 (Gemini): surveyId={}, reason={}", surveyId, e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            log.warn("LLM 요약 생성 중 예외 발생: surveyId={}", surveyId, e);
            return new ArrayList<>();
        }
    }

    private String buildPrompt(Long totalResponses,
                               OptionStatisticsDto topOption,
                               List<String> topKeywords,
                               List<String> sampleResponses,
                               int maxCount) {
        String topOptionText = topOption != null
                ? String.format("- 첫 객관식 주요 선택지: \"%s\" (%.2f%%)", topOption.getOptionContent(), topOption.getPercent())
                : "- 첫 객관식 주요 선택지: 없음 (객관식 응답 부족)";

        String keywordsText = (topKeywords == null || topKeywords.isEmpty())
                ? "- 주요 키워드: 없음"
                : "- 주요 키워드: " + String.join(", ", topKeywords);

        String samplesText;
        if (sampleResponses == null || sampleResponses.isEmpty()) {
            samplesText = "- 주관식 응답 샘플: 없음";
        } else {
            String joined = sampleResponses.stream()
                    .limit(20)
                    .map(s -> "• " + s)
                    .collect(Collectors.joining("\n"));
            samplesText = "- 주관식 응답 샘플:\n" + joined;
        }

        return String.format(
                "역할: 한국어 설문 분석가. 응답 데이터를 근거로 3~%d개의 핵심 인사이트를 bullet로 작성하라.\n" +
                        "규칙:\n" +
                        "- 추측/할루시네이션 금지, 근거가 없는 내용은 작성하지 말 것\n" +
                        "- 간결한 문장, 명령형/제안형으로 작성\n" +
                        "- 중복/동어 반복 금지, 숫자나 근거를 포함하면 좋음\n" +
                        "- 출력은 bullet 리스트만 작성 (항목 앞에 '- ' 또는 '• ' 사용)\n" +
                        "\n" +
                        "설문 메타:\n" +
                        "- 총 응답 세션 수: %d\n" +
                        "%s\n" +
                        "%s\n" +
                        "%s\n",
                maxCount,
                totalResponses != null ? totalResponses : 0,
                topOptionText,
                keywordsText,
                samplesText
        );
    }

    /**
     * Gemini 응답에서 bullet 항목을 추출한다.
     */
    private List<String> parseBullets(String raw, int maxCount) {
        if (raw == null || raw.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String[] lines = raw.split("\\r?\\n");
        List<String> bullets = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            // bullet prefix 제거
            if (trimmed.startsWith("- ")) {
                trimmed = trimmed.substring(2).trim();
            } else if (trimmed.startsWith("•")) {
                trimmed = trimmed.substring(1).trim();
            } else if (trimmed.matches("^\\d+[).]\\s.*")) {
                trimmed = trimmed.replaceFirst("^\\d+[).]\\s*", "").trim();
            }

            if (!trimmed.isEmpty()) {
                bullets.add(trimmed);
            }

            if (bullets.size() >= maxCount) {
                break;
            }
        }

        return bullets;
    }
}

