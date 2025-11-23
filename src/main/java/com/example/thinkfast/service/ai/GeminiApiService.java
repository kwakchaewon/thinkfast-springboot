package com.example.thinkfast.service.ai;

import com.example.thinkfast.dto.ai.GeminiRequest;
import com.example.thinkfast.dto.ai.GeminiResponse;
import com.example.thinkfast.exception.AiServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiApiService {

    private final WebClient webClient;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.api-url}")
    private String apiUrl;

    @Value("${gemini.timeout-seconds:30}")
    private int timeoutSeconds;

    /**
     * Gemini API를 호출하여 텍스트를 생성합니다.
     *
     * @param prompt 생성할 텍스트의 프롬프트
     * @return 생성된 텍스트
     * @throws AiServiceException API 호출 실패 시
     */
    public String generateText(String prompt) {
        try {
            GeminiRequest request = GeminiRequest.create(prompt);

            GeminiResponse response = webClient.post()
                    .uri(apiUrl)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("X-goog-api-key", apiKey)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GeminiResponse.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                            .filter(throwable -> throwable instanceof WebClientResponseException.TooManyRequests
                                    || throwable instanceof WebClientResponseException.ServiceUnavailable)
                            .doBeforeRetry(retrySignal -> log.warn("Gemini API 재시도: {}", retrySignal.totalRetries() + 1)))
                    .block();

            if (response == null) {
                throw new AiServiceException("Gemini API 응답이 null입니다.");
            }

            String text = response.getText();
            if (text == null || text.isEmpty()) {
                log.warn("Gemini API 응답에 텍스트가 없습니다. finishReason: {}", 
                    response.getCandidates() != null && !response.getCandidates().isEmpty() 
                        ? response.getCandidates().get(0).getFinishReason() : "unknown");
                throw new AiServiceException("Gemini API 응답에 텍스트가 없습니다.");
            }

            return text;
        } catch (WebClientResponseException e) {
            log.error("Gemini API 호출 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AiServiceException("Gemini API 호출 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Gemini API 호출 중 예외 발생", e);
            throw new AiServiceException("Gemini API 호출 중 예외 발생: " + e.getMessage(), e);
        }
    }

    /**
     * Gemini API를 비동기로 호출합니다.
     *
     * @param prompt 생성할 텍스트의 프롬프트
     * @return 생성된 텍스트를 포함한 Mono
     */
    public Mono<String> generateTextAsync(String prompt) {
        GeminiRequest request = GeminiRequest.create(prompt);

        return webClient.post()
                .uri(apiUrl)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header("X-goog-api-key", apiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GeminiResponse.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                        .filter(throwable -> throwable instanceof WebClientResponseException.TooManyRequests
                                || throwable instanceof WebClientResponseException.ServiceUnavailable))
                .map(response -> {
                    if (response == null) {
                        throw new AiServiceException("Gemini API 응답이 null입니다.");
                    }
                    String text = response.getText();
                    if (text == null || text.isEmpty()) {
                        throw new AiServiceException("Gemini API 응답에 텍스트가 없습니다.");
                    }
                    return text;
                })
                .doOnError(error -> log.error("Gemini API 비동기 호출 실패", error));
    }
}

