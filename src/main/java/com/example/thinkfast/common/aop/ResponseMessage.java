package com.example.thinkfast.common.aop;

public enum ResponseMessage {
    SUCCESS("요청이 성공적으로 처리되었습니다."),
    SURVEY_NOT_FOUND("존재하지 않는 설문입니다."),
    SURVEY_UNAVAILABLE("삭제 또는 만료된 설문입니다."),
    INVALID_REQUEST("잘못된 요청입니다."),
    INTERNAL_ERROR("서버 내부 오류가 발생했습니다."),
    UNAUTHORIZED("인증이 필요합니다."),
    ACCOUNT_ALREADY_EXISTS("이미 가입된 계정입니다."),
    RESPONSE_DUPLICATED("이미 제출한 응답입니다."),
    INVALID_CREDENTIALS("아이디 또는 비밀번호가 올바르지 않습니다."),
    INVALID_USERNAME("아이디가 올바르지 않습니다."),
    INVALID_PASSWORD("비밀번호가 올바르지 않습니다.");

    private final String message;

    ResponseMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
