package com.example.thinkfast.common.aop;

import com.example.thinkfast.common.annotation.PerformanceLog;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 성능 측정을 위한 AOP Aspect
 * @PerformanceLog 어노테이션이 적용된 메서드의 실행 시간을 측정하고 로깅합니다.
 */
@Aspect
@Component
public class PerformanceLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(PerformanceLoggingAspect.class);

    @Around("@annotation(com.example.thinkfast.common.annotation.PerformanceLog)")
    public Object logPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        PerformanceLog performanceLog = method.getAnnotation(PerformanceLog.class);

        long startTime = System.currentTimeMillis();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = method.getName();
        String fullMethodName = className + "." + methodName;

        Object result = null;
        Throwable exception = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable throwable) {
            exception = throwable;
            throw throwable;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            long slowThreshold = performanceLog.slowThresholdMs();
            boolean isSlow = duration > slowThreshold;

            // MDC 설정
            MDC.put("log_type", "performance");
            MDC.put("performance.class", className);
            MDC.put("performance.method", methodName);
            MDC.put("performance.duration_ms", String.valueOf(duration));
            MDC.put("performance.slow", String.valueOf(isSlow));

            // 파라미터 정보 (간단한 요약만)
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                StringBuilder params = new StringBuilder();
                for (int i = 0; i < Math.min(args.length, 3); i++) {
                    if (i > 0) params.append(", ");
                    if (args[i] != null) {
                        String argStr = args[i].toString();
                        // 민감 정보 마스킹
                        if (argStr.length() > 100) {
                            argStr = argStr.substring(0, 100) + "...";
                        }
                        params.append(argStr);
                    } else {
                        params.append("null");
                    }
                }
                if (args.length > 3) {
                    params.append("...");
                }
                MDC.put("performance.parameters", params.toString());
            }

            // 예외 발생 여부
            if (exception != null) {
                MDC.put("performance.exception", exception.getClass().getSimpleName());
                MDC.put("performance.success", "false");
            } else {
                MDC.put("performance.success", "true");
            }

            // 로깅
            String level = performanceLog.level().toUpperCase();
            String message = String.format("Performance: %s executed in %dms", fullMethodName, duration);

            if (exception != null) {
                log.error("{} (exception: {})", message, exception.getClass().getSimpleName(), exception);
            } else if (isSlow) {
                if ("WARN".equals(level)) {
                    log.warn("{} (slow)", message);
                } else {
                    log.info("{} (slow)", message);
                }
            } else {
                if ("DEBUG".equals(level)) {
                    log.debug(message);
                } else {
                    log.info(message);
                }
            }

            // MDC 정리
            MDC.remove("log_type");
            MDC.remove("performance.class");
            MDC.remove("performance.method");
            MDC.remove("performance.duration_ms");
            MDC.remove("performance.slow");
            MDC.remove("performance.parameters");
            MDC.remove("performance.exception");
            MDC.remove("performance.success");
        }
    }
}

