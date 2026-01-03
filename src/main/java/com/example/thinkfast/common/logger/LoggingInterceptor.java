package com.example.thinkfast.common.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import com.example.thinkfast.security.UserDetailImpl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Random;

@Component
public class LoggingInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);
    private static final String EXCEPTION_LOGGED = "exceptionLogged";
    private static final String START_TIME = "startTime";
    private static final long SLOW_REQUEST_THRESHOLD_MS = 3000; // 3초
    private static final double SUCCESS_REQUEST_SAMPLING_RATE = 0.1; // 10% 샘플링
    private static final Random random = new Random();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        long startTime = System.currentTimeMillis();
        request.setAttribute(START_TIME, startTime);

        // 사용자 ID 추출 및 MDC 설정
        String userId = extractUserId();
        if (userId != null) {
            MDC.put("user_id", userId);
        }

        // HTTP 요청 정보를 구조화된 로그로 출력
        MDC.put("log_type", "http_request");
        MDC.put("http.method", request.getMethod());
        MDC.put("http.uri", request.getRequestURI());
        MDC.put("http.client_ip", getClientIpAddress(request));
        MDC.put("http.user_agent", request.getHeader("User-Agent"));
        MDC.put("http.content_type", request.getContentType());

        log.info("HTTP request received: {} {}", request.getMethod(), request.getRequestURI());
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Object startTimeObj = request.getAttribute(START_TIME);
        if (startTimeObj == null) {
            return;
        }

        long startTime = (Long) startTimeObj;
        long duration = System.currentTimeMillis() - startTime;

        // HTTP 응답 정보 설정
        MDC.put("http.status", String.valueOf(response.getStatus()));
        MDC.put("http.duration_ms", String.valueOf(duration));
        
        // 응답 크기 추정 (실제 크기는 측정 불가하므로 헤더에서 추정)
        String contentLength = response.getHeader("Content-Length");
        if (contentLength != null) {
            MDC.put("http.response_size", contentLength);
        }

        // 느린 요청 감지
        boolean isSlowRequest = duration > SLOW_REQUEST_THRESHOLD_MS;
        if (isSlowRequest) {
            MDC.put("http.slow_request", "true");
            log.warn("Slow request detected: {} {} => {} ({}ms)", 
                    request.getMethod(), request.getRequestURI(), response.getStatus(), duration);
        }

        // Filter 단 중복 로깅 방지
        if (ex != null && request.getAttribute(EXCEPTION_LOGGED) == null) {
            MDC.put("log_type", "exception");
            log.error("HTTP request exception: {} {} => Exception: {}", 
                    request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
            request.setAttribute(EXCEPTION_LOGGED, true);
            MDC.remove("log_type");
        }

        // 정상 응답 로깅 (샘플링 적용)
        if (response.getStatus() < 400) {
            // 성공 요청은 10%만 로깅, 에러/경고/느린 요청은 100% 로깅
            if (isSlowRequest || random.nextDouble() < SUCCESS_REQUEST_SAMPLING_RATE) {
                MDC.put("log_type", "http_request");
                log.info("HTTP response: {} {} => {} ({}ms)", 
                        request.getMethod(), request.getRequestURI(), response.getStatus(), duration);
            }
        } else {
            // 에러 응답은 항상 로깅
            MDC.put("log_type", "http_request");
            log.warn("HTTP error response: {} {} => {} ({}ms)", 
                    request.getMethod(), request.getRequestURI(), response.getStatus(), duration);
        }

        // MDC 정리 (RequestIdFilter에서 이미 정리되지만 안전을 위해)
        MDC.remove("log_type");
        MDC.remove("http.method");
        MDC.remove("http.uri");
        MDC.remove("http.status");
        MDC.remove("http.duration_ms");
        MDC.remove("http.client_ip");
        MDC.remove("http.user_agent");
        MDC.remove("http.content_type");
        MDC.remove("http.response_size");
        MDC.remove("http.slow_request");
        MDC.remove("user_id");
    }

    /**
     * SecurityContext에서 사용자 ID 추출
     */
    private String extractUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserDetailImpl) {
                UserDetailImpl userDetail = (UserDetailImpl) authentication.getPrincipal();
                return userDetail.getUsername(); // User ID 대신 username 사용
            }
        } catch (Exception e) {
            // SecurityContext 접근 실패 시 무시
        }
        return null;
    }

    /**
     * 클라이언트 IP 주소 추출 (프록시 환경 고려)
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
