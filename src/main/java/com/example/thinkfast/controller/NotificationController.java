package com.example.thinkfast.controller;

import com.example.thinkfast.common.aop.BaseResponseBody;
import com.example.thinkfast.dto.survey.CreateSurveyRequest;
import com.example.thinkfast.realtime.ResponseCreatedAlarm;
import com.example.thinkfast.repository.NotificationRepository;
import com.example.thinkfast.security.UserDetailImpl;
import com.example.thinkfast.service.survey.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@BaseResponseBody
@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    /**
     * 개선 사항: 수정 및 업데이트 문들은 반드시 본인 계정에 해당하는 것인가에 대한 사전 검사 필요
     * @param surveyIds
     */
    @PostMapping("/read")
    @PreAuthorize("hasRole('CREATOR')")
    public void readSurveyNotification(@AuthenticationPrincipal UserDetailImpl userDetail,
                                       @RequestBody List<Long> surveyIds){
        notificationService.updateSurveyNotificationRead(userDetail, surveyIds);
    }

    @GetMapping
    @PreAuthorize("hasRole('CREATOR')")
    public List<ResponseCreatedAlarm> getNotificationSummaries(@AuthenticationPrincipal UserDetailImpl userDetail){
        return notificationService.getNotificationSummaries(userDetail);
    }
}
