package com.example.thinkfast.realtime;

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

    // 메시지 수신 후
    // Redis 발행된 (publish) 메시지를 받아서 websocket 을 통해 알림 전송하는 역할
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);

            AlarmMessage alarmMessage = objectMapper.readValue(json, AlarmMessage.class);
            List<ResponseCreatedAlarm> responseCreatedAlarms = alarmMessage.getNewResponseCreatedAlarms();

            // WebSocket을 통해 해당 userId에게 메시지 전송
            alarmHandler.sendToUser(alarmMessage.getUsername(), responseCreatedAlarms);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
