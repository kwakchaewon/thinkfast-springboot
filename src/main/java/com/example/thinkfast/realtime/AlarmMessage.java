package com.example.thinkfast.realtime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlarmMessage {
    private String username;
    private String message;
    private LocalDateTime createdAt;
}
