package com.example.thinkfast.service.survey;

import com.example.thinkfast.realtime.ResponseCreatedAlarm;
import com.example.thinkfast.repository.NotificationRepository;
import com.example.thinkfast.repository.auth.UserRepository;
import com.example.thinkfast.security.UserDetailImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    @Transactional
    public void updateSurveyNotificationRead(UserDetailImpl userDetail, List<Long> surveyIds){
        Long userId = userRepository.findIdByUsername(userDetail.getUsername());
        int result = notificationRepository.updateSurveyNotificationAsRead(userId, surveyIds);
    }

    public List<ResponseCreatedAlarm> getNotificationSummaries(UserDetailImpl userDetail){
        Long userId = userRepository.findIdByUsername(userDetail.getUsername());
        return notificationRepository.findNotificationSummariesByRecipient(userId);
    }
}
