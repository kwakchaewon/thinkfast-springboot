package com.example.thinkfast.controller;

import com.example.thinkfast.common.BaseResponseBody;
import com.example.thinkfast.dto.survey.CreateSurveyRequest;
import com.example.thinkfast.dto.survey.GetRecentSuveysResponse;
import com.example.thinkfast.security.UserDetailImpl;

import com.example.thinkfast.service.SurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@BaseResponseBody
@RestController
@RequestMapping("/survey")
@RequiredArgsConstructor
public class SurveyController {
    private final SurveyService surveyService;

    /**
     * 개선 사항: 요청 데이터 유효성 검사, Bulk Insert 를 통한 성능 최적화, 트랜잭션 전파 설정 
     * @param userDetail
     * @param createSurveyRequest
     */
    @PostMapping
    @PreAuthorize("hasRole('CREATOR')")
    public void createSurvey(@AuthenticationPrincipal UserDetailImpl userDetail,
                             @RequestBody CreateSurveyRequest createSurveyRequest){
        surveyService.createSurvey(userDetail, createSurveyRequest);
    }

    @GetMapping("/recent")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<List<GetRecentSuveysResponse>> getRecentSurveys(@AuthenticationPrincipal UserDetailImpl userDetail) {
        List<GetRecentSuveysResponse> recentSurveys = surveyService.getRecentSurveys(userDetail);
        return ResponseEntity.ok(recentSurveys);
    }
}
