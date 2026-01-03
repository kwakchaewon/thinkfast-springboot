package com.example.thinkfast.common.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

@Component
public class ExceptionLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(ExceptionLoggingFilter.class);
    private static final String EXCEPTION_LOGGED = "exceptionLogged";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 1. doFilter() 비정상 응답 감지 (Exception 및 Error 모두 캐치)
        try {
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            if (request.getAttribute(EXCEPTION_LOGGED) == null) {
                logStructuredException(request, ex, "exception");
                request.setAttribute(EXCEPTION_LOGGED, true);
            }
            throw ex; // 예외는 다시 던져줘야 정상적인 흐름 유지됨
        } catch (Error error) {
            // Error 타입도 캐치 (OutOfMemoryError 등)
            if (request.getAttribute(EXCEPTION_LOGGED) == null) {
                logStructuredException(request, error, "error");
                request.setAttribute(EXCEPTION_LOGGED, true);
            }
            throw error;
        }

        // 2. doFilter() 정상 응답 후 상태 코드만으로 문제 되는 경우 감지
        int status = response.getStatus();
        if ((status == 401 || status == 403 || status >= 500) && request.getAttribute(EXCEPTION_LOGGED) == null) {
            MDC.put("log_type", "exception");
            MDC.put("error.type", "HttpStatusException");
            MDC.put("error.message", "HTTP status code indicates error: " + status);
            MDC.put("error.severity", status >= 500 ? "critical" : "error");
            MDC.put("http.method", request.getMethod());
            MDC.put("http.uri", request.getRequestURI());
            MDC.put("http.status", String.valueOf(status));
            
            log.error("HTTP error status: {} {} => Status: {}", 
                    request.getMethod(), request.getRequestURI(), status);
            
            MDC.remove("log_type");
            MDC.remove("error.type");
            MDC.remove("error.message");
            MDC.remove("error.severity");
            MDC.remove("http.method");
            MDC.remove("http.uri");
            MDC.remove("http.status");
        }
    }

    /**
     * 구조화된 예외 로깅
     */
    private void logStructuredException(HttpServletRequest request, Throwable throwable, String type) {
        MDC.put("log_type", "exception");
        MDC.put("error.type", throwable.getClass().getName());
        MDC.put("error.message", throwable.getMessage() != null ? throwable.getMessage() : "No message");
        MDC.put("error.severity", determineSeverity(throwable));
        MDC.put("http.method", request.getMethod());
        MDC.put("http.uri", request.getRequestURI());

        // 스택 트레이스 추출
        String stackTrace = getStackTrace(throwable);
        MDC.put("error.stack_trace", stackTrace);

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

        if ("error".equals(type) || throwable instanceof Error) {
            log.error("System error occurred: {} {} => {}: {}", 
                    request.getMethod(), request.getRequestURI(), 
                    throwable.getClass().getSimpleName(), throwable.getMessage(), throwable);
        } else {
            log.error("Exception occurred: {} {} => {}: {}", 
                    request.getMethod(), request.getRequestURI(), 
                    throwable.getClass().getSimpleName(), throwable.getMessage(), throwable);
        }

        // MDC 정리
        MDC.remove("log_type");
        MDC.remove("error.type");
        MDC.remove("error.message");
        MDC.remove("error.severity");
        MDC.remove("error.stack_trace");
        MDC.remove("error.cause.type");
        MDC.remove("error.cause.message");
        MDC.remove("error.root_cause.type");
        MDC.remove("error.root_cause.message");
        MDC.remove("http.method");
        MDC.remove("http.uri");
    }

    /**
     * 예외 심각도 결정
     */
    private String determineSeverity(Throwable throwable) {
        if (throwable instanceof Error) {
            return "critical";
        }
        if (throwable instanceof OutOfMemoryError) {
            return "critical";
        }
        if (throwable instanceof StackOverflowError) {
            return "critical";
        }
        return "error";
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
}
