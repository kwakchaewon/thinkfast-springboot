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
    private List<ResponseCreatedAlarm> newResponseCreatedAlarms;
}
