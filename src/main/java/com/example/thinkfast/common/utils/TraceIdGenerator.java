package com.example.thinkfast.common.utils;

import java.util.UUID;

/**
 * 분산 추적을 위한 Trace ID 생성 유틸리티
 */
public class TraceIdGenerator {

    /**
     * 고유한 Trace ID 생성
     * 
     * @return UUID 기반 Trace ID
     */
    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 타임스탬프 기반 Trace ID 생성 (디버깅 용도)
     * 
     * @return 타임스탬프 기반 Trace ID
     */
    public static String generateWithTimestamp() {
        return "trace-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}

