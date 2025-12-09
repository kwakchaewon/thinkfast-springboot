package com.example.thinkfast.controller;

import com.example.thinkfast.common.aop.BaseResponseBody;
import com.example.thinkfast.realtime.dto.ResponseCreatedAlarm;
import com.example.thinkfast.security.UserDetailImpl;
import com.example.thinkfast.service.survey.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "알림 관리", description = "설문 응답 알림 조회 및 읽음 처리 API")
public class NotificationController {
    private final NotificationService notificationService;

    /**
     * 개선 사항: 수정 및 업데이트 문들은 반드시 본인 계정에 해당하는 것인가에 대한 사전 검사 필요
     * @param surveyIds
     */
    @Operation(summary = "알림 읽음 처리", description = "설문 응답 알림을 읽음 처리합니다. CREATOR 권한이 필요합니다.")
    @PostMapping("/read")
    @PreAuthorize("hasRole('CREATOR')")
    @SecurityRequirement(name = "bearerAuth")
    public void readSurveyNotification(@AuthenticationPrincipal UserDetailImpl userDetail,
                                       @RequestBody List<Long> surveyIds){
        notificationService.updateSurveyNotificationRead(userDetail, surveyIds);
    }

    @Operation(summary = "알림 목록 조회", description = "현재 사용자의 알림 목록을 조회합니다. CREATOR 권한이 필요합니다.")
    @GetMapping
    @PreAuthorize("hasRole('CREATOR')")
    @SecurityRequirement(name = "bearerAuth")
    public List<ResponseCreatedAlarm> getNotificationSummaries(@AuthenticationPrincipal UserDetailImpl userDetail){
        return notificationService.getNotificationSummaries(userDetail);
    }
}
