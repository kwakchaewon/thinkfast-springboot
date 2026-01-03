package com.example.thinkfast.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 성능 측정을 위한 메서드 어노테이션
 * 이 어노테이션이 적용된 메서드의 실행 시간이 자동으로 로깅됩니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PerformanceLog {
    /**
     * 느린 실행으로 간주할 임계값 (밀리초)
     * 기본값: 1000ms (1초)
     */
    long slowThresholdMs() default 1000;

    /**
     * 로그 레벨
     * 기본값: INFO
     */
    String level() default "INFO";
}

