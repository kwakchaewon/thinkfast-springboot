package com.example.thinkfast.common.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
@Component
public class LoggingInterceptor implements HandlerInterceptor {
    @Autowired
    private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        long startTime = System.currentTimeMillis();
        request.setAttribute("startTime", startTime);

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String clientIp = request.getRemoteAddr();

        log.info("[REQUEST] {} {} from {}", method, uri, clientIp);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        long startTime = (Long) request.getAttribute("startTime");
        long duration = System.currentTimeMillis() - startTime;

        String method = request.getMethod();
        String uri = request.getRequestURI();
        int status = response.getStatus();

        log.info("[RESPONSE] {} {} => {} ({}ms)", method, uri, status, duration);

        if (ex != null) {
            log.error("[EXCEPTION] {} {} => Exception: {}", method, uri, ex.getMessage(), ex);
        }
    }
}
