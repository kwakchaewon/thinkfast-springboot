package com.example.thinkfast.realtime.dto;

public enum NotificationMessage {
    SURVEY_RESPONSE("새로운 응답이 도착했습니다."),
    SURVEY_EXPIRED("설문이 만료됐습니다.");

    private final String message;

    NotificationMessage(String message) {
        this.message=message;
    }

    public String getMessage() {return message;}
}
