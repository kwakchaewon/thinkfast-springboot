package com.example.thinkfast.common.aop;

import com.example.thinkfast.exception.AiServiceException;
import com.example.thinkfast.exception.NoResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.example.thinkfast.security.UserDetailImpl;

import javax.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String EXCEPTION_LOGGED = "exceptionLogged";

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<BaseResponse> handleUsernameNotFoundException(
            HttpServletRequest request, UsernameNotFoundException e) {
        logStructuredException(request, e, "business", false);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(BaseResponse.fail(ResponseMessage.INVALID_CREDENTIALS));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<BaseResponse> handleBadCredentialsException(
            HttpServletRequest request, BadCredentialsException e) {
        logStructuredException(request, e, "business", false);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(BaseResponse.fail(ResponseMessage.INVALID_PASSWORD));
    }

    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<BaseResponse> handleAiServiceException(
            HttpServletRequest request, AiServiceException e) {
        logStructuredException(request, e, "system", true);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.fail("AI 서비스 처리 중 오류가 발생했습니다."));
    }

    @ExceptionHandler(NoResponseException.class)
    public ResponseEntity<BaseResponse> handleNoResponseException(
            HttpServletRequest request, NoResponseException e) {
        logStructuredException(request, e, "business", false);
        // 200 OK로 응답 (예외는 던지지 않지만, 혹시 모를 경우를 대비해 유지)
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.fail(ResponseMessage.NO_RESPONSE));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAllExceptions(HttpServletRequest request, Exception ex) {
        if (request.getAttribute(EXCEPTION_LOGGED) == null) {
            logStructuredException(request, ex, "system", true);
            request.setAttribute(EXCEPTION_LOGGED, true);
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("서버 내부 오류가 발생했습니다.");
    }

    /**
     * 구조화된 예외 로깅
     */
    private void logStructuredException(HttpServletRequest request, Throwable throwable, 
                                        String exceptionCategory, boolean includeStackTrace) {
        // 중복 로깅 방지
        if (request.getAttribute(EXCEPTION_LOGGED) != null) {
            return;
        }

        MDC.put("log_type", "exception");
        MDC.put("error.type", throwable.getClass().getName());
        MDC.put("error.message", throwable.getMessage() != null ? throwable.getMessage() : "No message");
        MDC.put("error.severity", exceptionCategory.equals("system") ? "error" : "warn");
        MDC.put("error.category", exceptionCategory); // business vs system
        MDC.put("http.method", request.getMethod());
        MDC.put("http.uri", request.getRequestURI());

        // 사용자 정보 추가
        String userId = extractUserId();
        if (userId != null) {
            MDC.put("user_id", userId);
        }

        // 요청 파라미터 정보 (민감 정보 제외)
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            MDC.put("http.query_string", maskSensitiveInfo(queryString));
        }

        // 스택 트레이스 (시스템 예외만)
        if (includeStackTrace) {
            String stackTrace = getStackTrace(throwable);
            MDC.put("error.stack_trace", stackTrace);
        }

        // 원인 예외 정보
        Throwable cause = throwable.getCause();
        if (cause != null) {
            MDC.put("error.cause.type", cause.getClass().getName());
            MDC.put("error.cause.message", cause.getMessage() != null ? cause.getMessage() : "No message");
        }

        // 최상위 원인 예외
        Throwable rootCause = getRootCause(throwable);
        if (rootCause != null && rootCause != throwable) {
            MDC.put("error.root_cause.type", rootCause.getClass().getName());
            MDC.put("error.root_cause.message", rootCause.getMessage() != null ? rootCause.getMessage() : "No message");
        }

        if (exceptionCategory.equals("system")) {
            log.error("System exception: {} {} => {}: {}", 
                    request.getMethod(), request.getRequestURI(), 
                    throwable.getClass().getSimpleName(), throwable.getMessage(), throwable);
        } else {
            log.warn("Business exception: {} {} => {}: {}", 
                    request.getMethod(), request.getRequestURI(), 
                    throwable.getClass().getSimpleName(), throwable.getMessage());
        }

        // MDC 정리
        MDC.remove("log_type");
        MDC.remove("error.type");
        MDC.remove("error.message");
        MDC.remove("error.severity");
        MDC.remove("error.category");
        MDC.remove("error.stack_trace");
        MDC.remove("error.cause.type");
        MDC.remove("error.cause.message");
        MDC.remove("error.root_cause.type");
        MDC.remove("error.root_cause.message");
        MDC.remove("http.method");
        MDC.remove("http.uri");
        MDC.remove("http.query_string");
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
                return userDetail.getUsername();
            }
        } catch (Exception e) {
            // SecurityContext 접근 실패 시 무시
        }
        return null;
    }

    /**
     * 민감 정보 마스킹
     */
    private String maskSensitiveInfo(String queryString) {
        // password, token, api_key 등의 파라미터 마스킹
        return queryString.replaceAll("(?i)(password|token|api_key|secret)=[^&]*", "$1=***");
    }

    /**
     * 스택 트레이스 문자열 추출
     */
    private String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * 최상위 원인 예외 추출
     */
    private Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable.getCause();
        if (cause == null || cause == throwable) {
            return throwable;
        }
        return getRootCause(cause);
    }


//    @ExceptionHandler(BadCredentialsException.class)
//    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(BadCredentialsException e) {
//        return ResponseEntity
//                .status(HttpStatus.UNAUTHORIZED)
//                .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "잘못된 인증 정보입니다."));
//    }
//
//    @ExceptionHandler(RuntimeException.class)
//    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException e) {
//        return ResponseEntity
//                .status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
//    }
//
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
//        return ResponseEntity
//                .status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."));
//    }
}