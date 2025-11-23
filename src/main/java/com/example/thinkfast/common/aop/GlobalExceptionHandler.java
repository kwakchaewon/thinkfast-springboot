package com.example.thinkfast.common.aop;

import com.example.thinkfast.exception.AiServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<BaseResponse> handleUsernameNotFoundException(UsernameNotFoundException e) {
        log.warn("아이디가 올바르지 않습니다.: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(BaseResponse.fail(ResponseMessage.INVALID_CREDENTIALS));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<BaseResponse> handleBadCredentialsException(BadCredentialsException e) {
        log.warn("비밀번호가 올바르지 않습니다.: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(BaseResponse.fail(ResponseMessage.INVALID_PASSWORD));
    }

    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<BaseResponse> handleAiServiceException(AiServiceException e) {
        log.error("AI 서비스 오류: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.fail("AI 서비스 처리 중 오류가 발생했습니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAllExceptions(HttpServletRequest request, Exception ex) {
        if (request.getAttribute("exceptionLogged") == null) {
            log.error("[EXCEPTION] {} {}", request.getMethod(), request.getRequestURI(), ex);
            request.setAttribute("exceptionLogged", true);
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("서버 내부 오류가 발생했습니다.");
    }


//    @ExceptionHandler(BadCredentialsException.class)
//    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(BadCredentialsException e) {
//        return ResponseEntity
//                .status(HttpStatus.UNAUTHORIZED)
//                .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "잘못된 인증 정보입니다."));
//    }
//
//    @ExceptionHandler(RuntimeException.class)
//    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException e) {
//        return ResponseEntity
//                .status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
//    }
//
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
//        return ResponseEntity
//                .status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."));
//    }
}