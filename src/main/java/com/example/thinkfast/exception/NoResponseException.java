package com.example.thinkfast.exception;

/**
 * 설문에 응답이 없을 때 발생하는 예외
 */
public class NoResponseException extends RuntimeException {
    public NoResponseException(String message) {
        super(message);
    }

    public NoResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}

