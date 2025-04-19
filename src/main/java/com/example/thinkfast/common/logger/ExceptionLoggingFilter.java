package com.example.thinkfast.common.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class ExceptionLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(ExceptionLoggingFilter.class);
    private static final String EXCEPTION_LOGGED = "exceptionLogged";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 1. doFilter() 비정상 응답 감지
        try {
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            if (request.getAttribute(EXCEPTION_LOGGED) == null){
                log.error("[EXCEPTION] {} {} => Exception: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
                request.setAttribute(EXCEPTION_LOGGED, true);
            }
            throw ex; // 예외는 다시 던져줘야 정상적인 흐름 유지됨
        }

        // 2. doFilter() 정상 응답 후 상태 코드만으로 문제 되는 경우 감지
        int status = response.getStatus();
        if ((status == 401 || status == 403 || status >= 500) && request.getAttribute(EXCEPTION_LOGGED) == null) {
            log.error("[EXCEPTION] {} {} => Status: {}", request.getMethod(), request.getRequestURI(), status);
        }
    }
}
