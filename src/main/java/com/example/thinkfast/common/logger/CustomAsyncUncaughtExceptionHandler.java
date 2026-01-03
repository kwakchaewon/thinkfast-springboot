package com.example.thinkfast.common.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 비동기 작업 예외 처리 핸들러
 * @Async 메서드에서 발생한 예외를 구조화된 로그로 기록
 */
public class CustomAsyncUncaughtExceptionHandler implements org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomAsyncUncaughtExceptionHandler.class);

    @Override
    public void handleUncaughtException(Throwable throwable, Method method, Object... params) {
        try {
            MDC.put("log_type", "exception");
            MDC.put("error.type", throwable.getClass().getName());
            MDC.put("error.message", throwable.getMessage() != null ? throwable.getMessage() : "No message");
            MDC.put("error.severity", "error");
            MDC.put("async.method", method.getDeclaringClass().getSimpleName() + "." + method.getName());
            MDC.put("async.thread", Thread.currentThread().getName());

            // 스택 트레이스 추출
            String stackTrace = getStackTrace(throwable);
            MDC.put("error.stack_trace", stackTrace);

            // 파라미터 정보 (간단한 요약만)
            if (params != null && params.length > 0) {
                StringBuilder paramStr = new StringBuilder();
                for (int i = 0; i < Math.min(params.length, 3); i++) {
                    if (i > 0) paramStr.append(", ");
                    if (params[i] != null) {
                        String param = params[i].toString();
                        if (param.length() > 50) {
                            param = param.substring(0, 50) + "...";
                        }
                        paramStr.append(param);
                    } else {
                        paramStr.append("null");
                    }
                }
                if (params.length > 3) {
                    paramStr.append("...");
                }
                MDC.put("async.parameters", paramStr.toString());
            }

            // 원인 예외 정보
            Throwable cause = throwable.getCause();
            if (cause != null) {
                MDC.put("error.cause.type", cause.getClass().getName());
                MDC.put("error.cause.message", cause.getMessage() != null ? cause.getMessage() : "No message");
            }

            log.error("Uncaught exception in async method: {} (thread: {})", 
                    method.getName(), Thread.currentThread().getName(), throwable);

            // MDC 정리
            MDC.remove("log_type");
            MDC.remove("error.type");
            MDC.remove("error.message");
            MDC.remove("error.severity");
            MDC.remove("error.stack_trace");
            MDC.remove("error.cause.type");
            MDC.remove("error.cause.message");
            MDC.remove("async.method");
            MDC.remove("async.thread");
            MDC.remove("async.parameters");
        } catch (Exception e) {
            log.error("Failed to log async exception", e);
        }
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
}

