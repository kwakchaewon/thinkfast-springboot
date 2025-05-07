package com.example.thinkfast.realtime;

import com.example.thinkfast.domain.Notification;
import com.example.thinkfast.repository.NotificationRepository;
import com.example.thinkfast.repository.auth.UserRepository;
import com.example.thinkfast.repository.survey.SurveyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
    public void sendAlarm(Long surveyId) {
        Long userId = surveyRepository.findUserIdById(surveyId);
        String username = userRepository.findUsernameById(userId);

        Notification notification = Notification.builder()
                .type("SURVEY_RESPONSE")
                .recipientId(userId)
                .message("새 응답이 도착했습니다.")
                .referenceId(surveyId)
                .build();

        Notification createdNotification = notificationRepository.save(notification);

        try {
            AlarmMessage alarmMessage = new AlarmMessage(username, createdNotification.getMessage(), createdNotification.getCreatedAt());
            String json = objectMapper.writeValueAsString(alarmMessage);
            // 레디스 채널에 매시지 전달
            redisTemplate.convertAndSend(CHANNEL, json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

