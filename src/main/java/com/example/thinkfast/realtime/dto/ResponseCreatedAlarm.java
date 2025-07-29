package com.example.thinkfast.realtime.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
public class ResponseCreatedAlarm {
    private String type;
    private Long surveyId;
    private String surveyTitle;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private long alarmCount;
}
