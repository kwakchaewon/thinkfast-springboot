package com.example.thinkfast.realtime;

import com.example.thinkfast.realtime.dto.AlarmMessage;
import com.example.thinkfast.realtime.dto.ResponseCreatedAlarm;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {
    private final AlarmHandler alarmHandler;
    private final ObjectMapper objectMapper;

    // publisher 로부터 받은 메시지를 웹 소켓을 통해 실시간 알림 전송 (알림 전송)
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);

            AlarmMessage alarmMessage = objectMapper.readValue(json, AlarmMessage.class);
            List<ResponseCreatedAlarm> responseCreatedAlarms = alarmMessage.getNewResponseCreatedAlarms();

            // 1. WebSocket을 통해 해당 username 기반 세션에 메시지 전송
            alarmHandler.sendToUser(alarmMessage.getUsername(), responseCreatedAlarms);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
