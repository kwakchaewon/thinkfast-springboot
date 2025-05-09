package com.example.thinkfast.realtime;

import com.example.thinkfast.domain.Notification;
import com.example.thinkfast.repository.NotificationRepository;
import com.example.thinkfast.repository.auth.UserRepository;
import com.example.thinkfast.repository.survey.SurveyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

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

        // 1. 알람 객체 생성
        Notification notification = Notification.builder()
                .type("SURVEY_RESPONSE")
                .recipientId(userId)
                .message("새 응답이 도착했습니다.")
                .referenceId(surveyId)
                .build();

        // 2. 알람 저장
        Notification createdNotification = notificationRepository.save(notification);

        // 3. 알람 저장 후 웹소켓에 전달할 메시지 (알람 리스트) 조회
        List<ResponseCreatedAlarm> responseCreatedAlarms = notificationRepository.findNotificationSummariesByRecipient(userId);
        AlarmMessage alarmMessage = new AlarmMessage(username, responseCreatedAlarms);

        try {
            String json = objectMapper.writeValueAsString(alarmMessage);

            // 4. 구독된 채널에 매시지 전달
            redisTemplate.convertAndSend(CHANNEL, json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

