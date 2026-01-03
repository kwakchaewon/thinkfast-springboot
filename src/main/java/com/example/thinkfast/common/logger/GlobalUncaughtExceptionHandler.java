package com.example.thinkfast.common.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 전역 UncaughtExceptionHandler
 * 비동기 작업 및 스레드에서 발생한 예외를 처리
 */
@Component
public class GlobalUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalUncaughtExceptionHandler.class);
    private final Thread.UncaughtExceptionHandler defaultHandler;

    public GlobalUncaughtExceptionHandler() {
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @PostConstruct
    public void init() {
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        try {
            logStructuredException(thread, throwable);
        } catch (Exception e) {
            // 로깅 중 예외 발생 시 기본 핸들러로 위임
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            } else {
                System.err.println("Uncaught exception in thread: " + thread.getName());
                throwable.printStackTrace();
            }
        }
    }

    /**
     * 구조화된 예외 로깅
     */
    private void logStructuredException(Thread thread, Throwable throwable) {
        MDC.put("log_type", "exception");
        MDC.put("error.type", throwable.getClass().getName());
        MDC.put("error.message", throwable.getMessage() != null ? throwable.getMessage() : "No message");
        MDC.put("error.severity", determineSeverity(throwable));
        MDC.put("thread.name", thread.getName());
        MDC.put("thread.id", String.valueOf(thread.getId()));
        MDC.put("thread.state", thread.getState().name());

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

        if (throwable instanceof Error) {
            log.error("Uncaught error in thread {}: {}: {}", 
                    thread.getName(), throwable.getClass().getSimpleName(), throwable.getMessage(), throwable);
        } else {
            log.error("Uncaught exception in thread {}: {}: {}", 
                    thread.getName(), throwable.getClass().getSimpleName(), throwable.getMessage(), throwable);
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
        MDC.remove("thread.name");
        MDC.remove("thread.id");
        MDC.remove("thread.state");
    }

    /**
     * 예외 심각도 결정
     */
    private String determineSeverity(Throwable throwable) {
        if (throwable instanceof OutOfMemoryError) {
            return "critical";
        }
        if (throwable instanceof StackOverflowError) {
            return "critical";
        }
        if (throwable instanceof Error) {
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

