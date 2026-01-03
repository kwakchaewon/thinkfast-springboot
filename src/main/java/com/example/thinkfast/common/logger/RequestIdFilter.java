package com.example.thinkfast.common.logger;

import com.example.thinkfast.common.utils.TraceIdGenerator;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * HTTP 요청마다 고유 Request ID 생성 및 MDC 설정
 * 요청 추적을 위한 필터
 */
@Component
@Order(1)
public class RequestIdFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String REQUEST_ID_MDC_KEY = "request_id";
    private static final String TRACE_ID_MDC_KEY = "trace_id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Request ID 생성 또는 헤더에서 추출
            String requestId = request.getHeader(REQUEST_ID_HEADER);
            if (requestId == null || requestId.isEmpty()) {
                requestId = "req-" + UUID.randomUUID().toString().replace("-", "");
            }

            // Trace ID 생성 (분산 추적용)
            String traceId = TraceIdGenerator.generate();

            // MDC에 설정
            MDC.put(REQUEST_ID_MDC_KEY, requestId);
            MDC.put(TRACE_ID_MDC_KEY, traceId);

            // 응답 헤더에 Request ID 추가
            response.setHeader(REQUEST_ID_HEADER, requestId);
            response.setHeader("X-Trace-ID", traceId);

            filterChain.doFilter(request, response);
        } finally {
            // 요청 완료 시 MDC 정리
            MDC.clear();
        }
    }
}

