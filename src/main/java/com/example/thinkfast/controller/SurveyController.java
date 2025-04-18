package com.example.thinkfast.controller;

import com.example.thinkfast.common.BaseResponseBody;
import com.example.thinkfast.dto.survey.CreateSurveyRequest;
import com.example.thinkfast.security.UserDetailImpl;
import com.example.thinkfast.service.AuthService;
import com.example.thinkfast.service.SurveyService;
import com.sun.security.auth.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@BaseResponseBody
@RestController
@RequestMapping("/survey")
@RequiredArgsConstructor
public class SurveyController {
    private final SurveyService surveyService;
    @PostMapping
    @PreAuthorize("hasRole('CREATOR')")
    public void createSurvey(@AuthenticationPrincipal UserDetailImpl userDetail,
                             @RequestBody CreateSurveyRequest createSurveyRequest){
        surveyService.createSurvey(userDetail, createSurveyRequest);
    }

}
