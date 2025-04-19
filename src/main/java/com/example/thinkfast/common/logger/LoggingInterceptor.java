package com.example.thinkfast.common.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
@Component
public class LoggingInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);
    private static final String EXCEPTION_LOGGED = "exceptionLogged";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("startTime", System.currentTimeMillis());

        log.info("[REQUEST] {} {} from {}", request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        long duration = System.currentTimeMillis() - (Long) request.getAttribute("startTime");

        // Filter 단 중복 로깅 방지
        if (ex != null && request.getAttribute(EXCEPTION_LOGGED) == null) {
            log.error("[EXCEPTION] {} {} => Exception: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
            request.setAttribute(EXCEPTION_LOGGED, true);
        }

        log.info("[RESPONSE] {} {} => {} ({}ms)", request.getMethod(), request.getRequestURI(), response.getStatus(), duration);
    }
}
