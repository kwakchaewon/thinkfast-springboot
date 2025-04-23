package com.example.thinkfast.controller;

import com.example.thinkfast.common.BaseResponseBody;
import com.example.thinkfast.domain.survey.Question;
import com.example.thinkfast.domain.survey.Survey;
import com.example.thinkfast.dto.survey.CreateSurveyRequest;
import com.example.thinkfast.dto.survey.GetRecentSurveysResponse;
import com.example.thinkfast.dto.survey.GetSurveyDetailResponse;
import com.example.thinkfast.repository.survey.QuestionRepository;
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
    private final QuestionRepository questionRepository;

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

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CREATOR')")
    public void deleteSurvey(@AuthenticationPrincipal UserDetailImpl userDetail,
                             @PathVariable Long id){
        surveyService.deleteSurvey(id);
    }

    @GetMapping("/recent")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<List<GetRecentSurveysResponse>> getRecentSurveys(@AuthenticationPrincipal UserDetailImpl userDetail) {
        List<GetRecentSurveysResponse> recentSurveys = surveyService.getRecentSurveys(userDetail);
        return ResponseEntity.ok(recentSurveys);
    }

    /**
     * 개선사항: DELETED 설문에 대한 예외처리 필요
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<GetSurveyDetailResponse> getRecentSurveys(@PathVariable Long id) {
        Survey survey = surveyService.getSurveyDetail(id);
        List<Question> questions = questionRepository.findBySurveyId(survey.getId());
        GetSurveyDetailResponse getSurveyDetailResponse = new GetSurveyDetailResponse(survey, questions);
        return ResponseEntity.ok(getSurveyDetailResponse);
    }
}
