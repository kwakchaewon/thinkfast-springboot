package com.example.thinkfast.controller;

import com.example.thinkfast.common.BaseResponse;
import com.example.thinkfast.common.BaseResponseBody;
import com.example.thinkfast.domain.survey.Question;
import com.example.thinkfast.dto.survey.CreateSurveyRequest;
import com.example.thinkfast.dto.survey.GetRecentSurveysResponse;
import com.example.thinkfast.dto.survey.GetSurveyDetailResponse;
import com.example.thinkfast.dto.survey.QuestionDto;
import com.example.thinkfast.security.UserDetailImpl;

import com.example.thinkfast.service.SurveyService;
import com.example.thinkfast.service.survey.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@BaseResponseBody
@RestController
@RequestMapping("/survey")
@RequiredArgsConstructor
/**
 * 개선 사항: 호출 전 본인 권한 확인 필요
 */
public class SurveyController {
    private final SurveyService surveyService;
    private final QuestionService questionService;

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
    public void deleteSurvey(@PathVariable Long id){
        surveyService.deleteSurvey(id);
    }

    @GetMapping("/recent")
    @PreAuthorize("hasRole('CREATOR')")
    public List<GetRecentSurveysResponse> getRecentSurveys(@AuthenticationPrincipal UserDetailImpl userDetail) {
        return surveyService.getRecentSurveys(userDetail);
    }

    /**
     * 개선사항: DELETED 설문에 대한 예외처리 필요
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CREATOR')")
    public BaseResponse<GetSurveyDetailResponse> getSurveyDetail(@PathVariable Long id) {
        GetSurveyDetailResponse getSurveyDetailResponse = surveyService.getSurveyDetail(id);
        if (getSurveyDetailResponse == null) return BaseResponse.fail("존재하지 않는 설문입니다.");
        return BaseResponse.success(getSurveyDetailResponse);
    }

    /**
     * 개선 사항: 조회 속도 개선 필요
     * @param surveyId
     * @return
     */
    @GetMapping("/{surveyId}/questions")
    public BaseResponse<List<QuestionDto>> getQuestionsBySurveyId(@PathVariable Long surveyId) {
        // 1. 설문 기반 question 리스트 조회

        List<Question> questions = questionService.getIdsBySurveyId(surveyId);
        if (questions.isEmpty()){
            return BaseResponse.fail("존재하지 않는 설문입니다.");
        }

        List<QuestionDto> questionDtos = new ArrayList<>();;

        // 2. question id 기반 survey + option 조회
        for (Question question : questions){
            QuestionDto questionDto = questionService.getQuestionWithOptions(question.getId());
            questionDtos.add(questionDto);
        }

        return BaseResponse.success(questionDtos);
    }


}
