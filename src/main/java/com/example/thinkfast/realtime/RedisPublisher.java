package com.example.thinkfast.realtime;

import com.example.thinkfast.domain.Notification;
import com.example.thinkfast.realtime.dto.AlarmMessage;
import com.example.thinkfast.realtime.dto.NotificationMessage;
import com.example.thinkfast.realtime.dto.ResponseCreatedAlarm;
import com.example.thinkfast.repository.NotificationRepository;
import com.example.thinkfast.repository.auth.UserRepository;
import com.example.thinkfast.repository.survey.SurveyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPublisher {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SurveyRepository surveyRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    private final String CHANNEL = "alarm-channel";

    /**
     * 개선 사항: 알람 저장 및 확인된 알람 status 변경 로직
     * @param surveyId
     */
    public void sendAlarm(Long surveyId, String type) {
        long startTime = System.currentTimeMillis();
        
        try {
            MDC.put("log_type", "external_api");
            MDC.put("external_api.system", "redis");
            MDC.put("external_api.operation", "pubsub_send");
            MDC.put("external_api.channel", CHANNEL);

            Long userId = surveyRepository.findUserIdById(surveyId);
            String username = userRepository.findUsernameById(userId);
            String realUsername = userRepository.findRealUsernameById(userId);
            
            // realUsername이 있으면 realUsername 사용, 없으면 username 사용
            String displayName = (realUsername != null && !realUsername.isEmpty()) 
                    ? realUsername 
                    : username;

            String message = String.valueOf(NotificationMessage.valueOf(type));

            // 1. 알람 객체 생성
            Notification notification = Notification.builder()
                    .type(type)
                    .recipientId(userId)
                    .message(message)
                    .referenceId(surveyId)
                    .build();

            // 2. 알람 저장
            Notification createdNotification = notificationRepository.save(notification);

            // 3. 알람 저장 후 웹소켓에 전달할 메시지 (최신 30일 알람 리스트. 추후 7일로 변경 예정) 조회
            LocalDateTime monthAgo = LocalDateTime.now().minusDays(30);
            List<ResponseCreatedAlarm> responseCreatedAlarms = notificationRepository.findNotificationSummariesByRecipient(userId, monthAgo);
            AlarmMessage alarmMessage = new AlarmMessage(username, displayName, responseCreatedAlarms);

            String json = objectMapper.writeValueAsString(alarmMessage);
            int messageSize = json.length();

            // 4. 구독된 채널에 매시지 전달
            redisTemplate.convertAndSend(CHANNEL, json);

            long duration = System.currentTimeMillis() - startTime;
            MDC.put("external_api.duration_ms", String.valueOf(duration));
            MDC.put("external_api.status", "success");
            MDC.put("external_api.message_size", String.valueOf(messageSize));
            MDC.put("external_api.recipient_id", String.valueOf(userId));

            log.info("Redis Pub/Sub message sent: channel={}, recipient={}, size={} bytes ({}ms)", 
                    CHANNEL, userId, messageSize, duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            MDC.put("external_api.duration_ms", String.valueOf(duration));
            MDC.put("external_api.status", "failure");
            MDC.put("external_api.error_message", e.getMessage());
            
            log.error("Redis Pub/Sub message send failed: channel={}, surveyId={} ({}ms)", 
                    CHANNEL, surveyId, duration, e);
        } finally {
            // MDC 정리
            MDC.remove("log_type");
            MDC.remove("external_api.system");
            MDC.remove("external_api.operation");
            MDC.remove("external_api.channel");
            MDC.remove("external_api.duration_ms");
            MDC.remove("external_api.status");
            MDC.remove("external_api.message_size");
            MDC.remove("external_api.recipient_id");
            MDC.remove("external_api.error_message");
        }
    }
}

