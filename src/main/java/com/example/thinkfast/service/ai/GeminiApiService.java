package com.example.thinkfast.service.ai;

import com.example.thinkfast.dto.ai.GeminiRequest;
import com.example.thinkfast.dto.ai.GeminiResponse;
import com.example.thinkfast.exception.AiServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;

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
        Instant startTime = Instant.now();
        int retryCount = 0;
        
        try {
            GeminiRequest request = GeminiRequest.create(prompt);

            // 구조화된 로깅을 위한 MDC 설정
            MDC.put("log_type", "external_api");
            MDC.put("external_api.system", "gemini");
            MDC.put("external_api.operation", "generateContent");
            MDC.put("external_api.request_url", apiUrl);

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
                            .doBeforeRetry(retrySignal -> {
                                retryCount = retrySignal.totalRetries() + 1;
                                MDC.put("external_api.retry_count", String.valueOf(retryCount));
                                log.warn("Gemini API 재시도: {}", retryCount);
                            }))
                    .block();

            long duration = Duration.between(startTime, Instant.now()).toMillis();
            MDC.put("external_api.duration_ms", String.valueOf(duration));
            MDC.put("external_api.retry_count", String.valueOf(retryCount));

            if (response == null) {
                MDC.put("external_api.status", "failure");
                MDC.put("external_api.error_message", "Response is null");
                log.error("Gemini API 응답이 null입니다. ({}ms, retries: {})", duration, retryCount);
                throw new AiServiceException("Gemini API 응답이 null입니다.");
            }

            String text = response.getText();
            if (text == null || text.isEmpty()) {
                String finishReason = response.getCandidates() != null && !response.getCandidates().isEmpty() 
                    ? response.getCandidates().get(0).getFinishReason() : "unknown";
                MDC.put("external_api.status", "failure");
                MDC.put("external_api.error_message", "Empty response text. finishReason: " + finishReason);
                log.warn("Gemini API 응답에 텍스트가 없습니다. finishReason: {} ({}ms, retries: {})", 
                    finishReason, duration, retryCount);
                throw new AiServiceException("Gemini API 응답에 텍스트가 없습니다.");
            }

            MDC.put("external_api.status", "success");
            MDC.put("external_api.response_status", "200");
            log.info("Gemini API 호출 성공: generateContent ({}ms, retries: {})", duration, retryCount);

            return text;
        } catch (WebClientResponseException e) {
            long duration = Duration.between(startTime, Instant.now()).toMillis();
            MDC.put("external_api.duration_ms", String.valueOf(duration));
            MDC.put("external_api.status", "failure");
            MDC.put("external_api.response_status", String.valueOf(e.getStatusCode().value()));
            MDC.put("external_api.error_message", e.getMessage());
            MDC.put("external_api.retry_count", String.valueOf(retryCount));
            
            log.error("Gemini API 호출 실패: status={}, body={} ({}ms, retries: {})", 
                    e.getStatusCode(), e.getResponseBodyAsString(), duration, retryCount);
            throw new AiServiceException("Gemini API 호출 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            long duration = Duration.between(startTime, Instant.now()).toMillis();
            MDC.put("external_api.duration_ms", String.valueOf(duration));
            MDC.put("external_api.status", "failure");
            MDC.put("external_api.error_message", e.getMessage());
            MDC.put("external_api.retry_count", String.valueOf(retryCount));
            
            log.error("Gemini API 호출 중 예외 발생 ({}ms, retries: {})", duration, retryCount, e);
            throw new AiServiceException("Gemini API 호출 중 예외 발생: " + e.getMessage(), e);
        } finally {
            // MDC 정리
            MDC.remove("log_type");
            MDC.remove("external_api.system");
            MDC.remove("external_api.operation");
            MDC.remove("external_api.request_url");
            MDC.remove("external_api.duration_ms");
            MDC.remove("external_api.status");
            MDC.remove("external_api.response_status");
            MDC.remove("external_api.error_message");
            MDC.remove("external_api.retry_count");
        }
    }

    /**
     * Gemini API를 비동기로 호출합니다.
     *
     * @param prompt 생성할 텍스트의 프롬프트
     * @return 생성된 텍스트를 포함한 Mono
     */
    public Mono<String> generateTextAsync(String prompt) {
        Instant startTime = Instant.now();
        GeminiRequest request = GeminiRequest.create(prompt);

        // 구조화된 로깅을 위한 MDC 설정
        MDC.put("log_type", "external_api");
        MDC.put("external_api.system", "gemini");
        MDC.put("external_api.operation", "generateContent");
        MDC.put("external_api.request_url", apiUrl);

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
                                || throwable instanceof WebClientResponseException.ServiceUnavailable)
                        .doBeforeRetry(retrySignal -> {
                            MDC.put("external_api.retry_count", String.valueOf(retrySignal.totalRetries() + 1));
                            log.warn("Gemini API 비동기 재시도: {}", retrySignal.totalRetries() + 1);
                        }))
                .map(response -> {
                    long duration = Duration.between(startTime, Instant.now()).toMillis();
                    MDC.put("external_api.duration_ms", String.valueOf(duration));
                    
                    if (response == null) {
                        MDC.put("external_api.status", "failure");
                        MDC.put("external_api.error_message", "Response is null");
                        log.error("Gemini API 비동기 호출: 응답이 null입니다. ({}ms)", duration);
                        throw new AiServiceException("Gemini API 응답이 null입니다.");
                    }
                    String text = response.getText();
                    if (text == null || text.isEmpty()) {
                        MDC.put("external_api.status", "failure");
                        MDC.put("external_api.error_message", "Empty response text");
                        log.warn("Gemini API 비동기 호출: 응답에 텍스트가 없습니다. ({}ms)", duration);
                        throw new AiServiceException("Gemini API 응답에 텍스트가 없습니다.");
                    }
                    
                    MDC.put("external_api.status", "success");
                    MDC.put("external_api.response_status", "200");
                    log.info("Gemini API 비동기 호출 성공: generateContent ({}ms)", duration);
                    return text;
                })
                .doOnError(error -> {
                    long duration = Duration.between(startTime, Instant.now()).toMillis();
                    MDC.put("external_api.duration_ms", String.valueOf(duration));
                    MDC.put("external_api.status", "failure");
                    MDC.put("external_api.error_message", error.getMessage());
                    log.error("Gemini API 비동기 호출 실패 ({}ms)", duration, error);
                })
                .doFinally(signalType -> {
                    // MDC 정리
                    MDC.remove("log_type");
                    MDC.remove("external_api.system");
                    MDC.remove("external_api.operation");
                    MDC.remove("external_api.request_url");
                    MDC.remove("external_api.duration_ms");
                    MDC.remove("external_api.status");
                    MDC.remove("external_api.response_status");
                    MDC.remove("external_api.error_message");
                    MDC.remove("external_api.retry_count");
                });
    }
}

