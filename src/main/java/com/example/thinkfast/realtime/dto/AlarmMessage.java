package com.example.thinkfast.realtime.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlarmMessage {
    private String username;
    private String displayName; // realUsername 우선, 없으면 username
    private List<ResponseCreatedAlarm> newResponseCreatedAlarms;
}
