package com.example.thinkfast.controller;

import com.example.thinkfast.common.aop.BaseResponse;
import com.example.thinkfast.common.aop.BaseResponseBody;
import com.example.thinkfast.common.aop.ResponseMessage;
import com.example.thinkfast.common.utils.IpUtil;
import com.example.thinkfast.domain.survey.Question;
import com.example.thinkfast.dto.survey.CreateResponseRequest;
import com.example.thinkfast.dto.survey.CreateSurveyRequest;
import com.example.thinkfast.dto.survey.GetRecentSurveysResponse;
import com.example.thinkfast.dto.survey.GetSurveyDetailResponse;
import com.example.thinkfast.dto.survey.PublicSurveyListResponse;
import com.example.thinkfast.dto.survey.QuestionDto;
import com.example.thinkfast.realtime.RedisPublisher;
import com.example.thinkfast.security.UserDetailImpl;

import com.example.thinkfast.service.survey.SurveyService;
import com.example.thinkfast.service.survey.ResponseService;
import com.example.thinkfast.service.survey.QuestionService;
import com.example.thinkfast.service.ai.SummaryService;
import com.example.thinkfast.dto.ai.SummaryReportDto;
import com.example.thinkfast.service.ai.WordCloudService;
import com.example.thinkfast.dto.ai.WordCloudResponseDto;
import com.example.thinkfast.service.ai.InsightService;
import com.example.thinkfast.service.ai.SurveyStatisticsService;
import com.example.thinkfast.dto.ai.QuestionStatisticsResponseDto;
import com.example.thinkfast.domain.survey.Survey;
import com.example.thinkfast.repository.auth.UserRepository;
import com.example.thinkfast.repository.survey.SurveyRepository;
import com.example.thinkfast.repository.survey.QuestionRepository;
import com.example.thinkfast.dto.survey.QuestionResponsesResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
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
    private final ResponseService responseService;
    private final RedisPublisher redisPublisher;
    private final SummaryService summaryService;
    private final WordCloudService wordCloudService;
    private final InsightService insightService;
    private final SurveyStatisticsService statisticsService;
    private final UserRepository userRepository;
    private final SurveyRepository surveyRepository;
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
    public void deleteSurvey(@PathVariable Long id){
        surveyService.deleteSurvey(id);
    }

    /**
     * 공개 설문 목록 조회
     * 인증 불필요, 모든 사용자가 접근 가능
     *
     * @param page   페이지 번호 (기본값: 1)
     * @param size   한 페이지당 항목 수 (기본값: 10, 최대: 100)
     * @param sort   정렬 기준 (newest, oldest, responses)
     * @param search 검색 키워드 (제목/설명/작성자명)
     * @return 공개 설문 목록 및 페이징 정보
     */
    @GetMapping("/public")
    public BaseResponse<PublicSurveyListResponse> getPublicSurveys(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(required = false) String search) {
        PublicSurveyListResponse response = surveyService.getPublicSurveys(page, size, sort, search);
        return BaseResponse.success(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('CREATOR')")
    public List<GetRecentSurveysResponse> getSurveys(@AuthenticationPrincipal UserDetailImpl userDetail) {
        return surveyService.getSurveys(userDetail);
    }

    /**
     * 개선사항: 설문 응답 count 인덱싱 적용
     * @param userDetail
     * @return
     */
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
        if (getSurveyDetailResponse == null) return BaseResponse.fail(ResponseMessage.SURVEY_NOT_FOUND);
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
            return BaseResponse.fail(ResponseMessage.SURVEY_NOT_FOUND);
        }

        List<QuestionDto> questionDtos = new ArrayList<>();;

        // 2. question id 기반 survey + option 조회
        for (Question question : questions){
            QuestionDto questionDto = questionService.getQuestionWithOptions(question.getId());
            questionDtos.add(questionDto);
        }

        return BaseResponse.success(questionDtos);
    }

    /**
     * 응답 방식: 비회원, 무인증 참여 가능
     * 개선 사항1: 쿠키, 로컬 스토리지, IP, 디바이스 ID 등을 통한 중복 응답 방지 (추후 관련 칼럼 추가)
     * 개선 사항2: 알람 메시지 DB 저장 후 read, unread 등 status 관리
     * @param createResponseRequest
     */
    @PostMapping("/{surveyId}/responses")
    @Transactional
    public BaseResponse createResponse(@PathVariable Long surveyId, @AuthenticationPrincipal UserDetailImpl userDetail,
                                       @RequestBody CreateResponseRequest createResponseRequest, HttpServletRequest request) {
        String clientIpAddress = IpUtil.getClientIp(request);
        
        String deviceId = createResponseRequest.getClientInfo() != null 
            ? createResponseRequest.getClientInfo().getDeviceId() 
            : null;
        
        log.info("[응답 생성 요청] surveyId={}, deviceId={}, ipAddress={}, clientInfo={}", 
            surveyId,
            deviceId != null ? (deviceId.length() > 20 ? deviceId.substring(0, 20) + "..." : deviceId) : "null",
            clientIpAddress,
            createResponseRequest.getClientInfo() != null ? "존재" : "null");

        if (surveyService.isSurveyInactive(surveyId)) {
            log.warn("[응답 생성 실패] surveyId={}, 사유=설문 비활성화 또는 삭제됨", surveyId);
            return BaseResponse.fail(ResponseMessage.SURVEY_UNAVAILABLE);
        }
        
        // 중복 응답 방지: 같은 설문에 대해 같은 deviceId/IP로는 한 번만 응답 가능
        // deviceId나 ipAddress가 null/빈 값이면 중복 체크를 건너뜀
        if (surveyService.isDuplicateResponse(surveyId, deviceId, clientIpAddress)) {
            log.warn("[응답 생성 실패] surveyId={}, deviceId={}, ipAddress={}, 사유=중복 응답", 
                surveyId,
                deviceId != null ? (deviceId.length() > 20 ? deviceId.substring(0, 20) + "..." : deviceId) : "null",
                clientIpAddress);
            return BaseResponse.fail(ResponseMessage.RESPONSE_DUPLICATED);
        }
        
        log.info("[응답 생성 시작] surveyId={}, deviceId={}, ipAddress={}", 
            surveyId,
            deviceId != null ? (deviceId.length() > 20 ? deviceId.substring(0, 20) + "..." : deviceId) : "null",
            clientIpAddress);
        
        responseService.createResponse(userDetail, surveyId, clientIpAddress, createResponseRequest);
        redisPublisher.sendAlarm(surveyId, "SURVEY_RESPONSE");
        
        log.info("[응답 생성 완료] surveyId={}, deviceId={}, ipAddress={}", 
            surveyId,
            deviceId != null ? (deviceId.length() > 20 ? deviceId.substring(0, 20) + "..." : deviceId) : "null",
            clientIpAddress);
        
        return BaseResponse.success();
    }

    /**
     * 설문 요약 리포트 조회
     * 설문 소유자만 조회 가능
     *
     * @param id 설문 ID
     * @param userDetail 현재 사용자 정보
     * @return 요약 리포트 (mainPosition, mainPositionPercent, improvements)
     */
    @GetMapping("/{id}/summary")
    public BaseResponse<SummaryReportDto> getSummaryReport(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailImpl userDetail) {
        
        // 1. 설문 존재 여부 확인
        Optional<Survey> surveyOpt = surveyRepository.findById(id);
        if (!surveyOpt.isPresent() || surveyOpt.get().getIsDeleted()) {
            return BaseResponse.fail(ResponseMessage.SURVEY_NOT_FOUND);
        }
        
        Survey survey = surveyOpt.get();

        // 2-1. 인증 유저 지만 비공개 설문 조회 시, userId 가 다를 경우 UNAUTHORIZED
        if (userDetail!=null){
            Long currentUserId = userRepository.findIdByUsername(userDetail.getUsername());
            if (survey.getShowResults().equals(false) && !survey.getUserId().equals(currentUserId)) {
                return BaseResponse.fail(ResponseMessage.UNAUTHORIZED);
            }
        }

        // 2-2. 비인증 유저가 비공개 설문 조회 시, UNAUTHORIZED
        if (userDetail==null && survey.getShowResults().equals(false)){
            return BaseResponse.fail(ResponseMessage.UNAUTHORIZED);
        }


        // 3. 요약 리포트 조회 (DB 우선, 없으면 실시간 생성)
        SummaryReportDto summaryReport = summaryService.getSummaryReport(id);
        
        return BaseResponse.success(summaryReport);
    }

    /**
     * 워드클라우드 조회
     * 설문 소유자만 조회 가능
     * 주관식 질문에만 적용
     *
     * @param surveyId 설문 ID
     * @param questionId 질문 ID
     * @param userDetail 현재 사용자 정보
     * @return 워드클라우드 데이터
     */
    @GetMapping("/{surveyId}/questions/{questionId}/wordcloud")
    public BaseResponse<WordCloudResponseDto> getWordCloud(
            @PathVariable Long surveyId,
            @PathVariable Long questionId,
            @AuthenticationPrincipal UserDetailImpl userDetail) {
        
        // 1. 설문 존재 여부 및 소유자 확인
        Optional<Survey> surveyOpt = surveyRepository.findById(surveyId);
        if (!surveyOpt.isPresent() || surveyOpt.get().getIsDeleted()) {
            return BaseResponse.fail(ResponseMessage.SURVEY_NOT_FOUND);
        }
        
        Survey survey = surveyOpt.get();
        Long currentUserId = userRepository.findIdByUsername(userDetail.getUsername());
        if (!survey.getUserId().equals(currentUserId)) {
            return BaseResponse.fail(ResponseMessage.UNAUTHORIZED);
        }
        
        // 2. 질문 존재 여부 및 타입 확인
        Optional<Question> questionOpt = questionRepository.findById(questionId);
        if (!questionOpt.isPresent()) {
            return BaseResponse.fail(ResponseMessage.SURVEY_NOT_FOUND);
        }
        
        Question question = questionOpt.get();
        if (question.getType() != Question.QuestionType.SUBJECTIVE) {
            return BaseResponse.fail(ResponseMessage.INVALID_REQUEST);
        }
        
        // 3. 워드클라우드 조회 (DB 우선, 없으면 실시간 생성)
        WordCloudResponseDto wordCloud = wordCloudService.getWordCloud(questionId);
        
        return BaseResponse.success(wordCloud);
    }

    /**
     * 인사이트 텍스트 조회
     * 설문 소유자만 조회 가능
     * 객관식 및 주관식 질문에 적용 (척도형 제외)
     *
     * @param surveyId 설문 ID
     * @param questionId 질문 ID
     * @param userDetail 현재 사용자 정보
     * @return 인사이트 텍스트
     */
    @GetMapping("/{surveyId}/questions/{questionId}/insight")
    public BaseResponse<String> getInsight(
            @PathVariable Long surveyId,
            @PathVariable Long questionId,
            @AuthenticationPrincipal UserDetailImpl userDetail) {
        
        // 1. 설문 존재 여부 및 소유자 확인
        Optional<Survey> surveyOpt = surveyRepository.findById(surveyId);
        if (!surveyOpt.isPresent() || surveyOpt.get().getIsDeleted()) {
            return BaseResponse.fail(ResponseMessage.SURVEY_NOT_FOUND);
        }
        
        Survey survey = surveyOpt.get();
        Long currentUserId = userRepository.findIdByUsername(userDetail.getUsername());
        if (!survey.getUserId().equals(currentUserId)) {
            return BaseResponse.fail(ResponseMessage.UNAUTHORIZED);
        }
        
        // 2. 질문 존재 여부 확인
        Optional<Question> questionOpt = questionRepository.findById(questionId);
        if (!questionOpt.isPresent()) {
            return BaseResponse.fail(ResponseMessage.SURVEY_NOT_FOUND);
        }
        
        Question question = questionOpt.get();
        
        // 3. 질문이 해당 설문에 속하는지 확인
        if (!question.getSurveyId().equals(surveyId)) {
            return BaseResponse.fail(ResponseMessage.INVALID_REQUEST);
        }
        
        // 4. 인사이트 조회 (DB 우선, 없으면 실시간 생성)
        String insight = insightService.getInsight(questionId);
        
        return BaseResponse.success(insight);
    }

    /**
     * 질문별 통계 조회
     * 설문 소유자만 조회 가능
     * 객관식 질문의 경우 각 선택지별 응답 수와 비율을 반환
     * 주관식 질문의 경우 전체 응답 수만 반환
     *
     * @param surveyId 설문 ID
     * @param questionId 질문 ID
     * @param userDetail 현재 사용자 정보
     * @return 질문별 통계 데이터
     */
    @GetMapping("/{surveyId}/questions/{questionId}/statistics")
    public BaseResponse<QuestionStatisticsResponseDto> getQuestionStatistics(
            @PathVariable Long surveyId,
            @PathVariable Long questionId,
            @AuthenticationPrincipal UserDetailImpl userDetail) {
        
        try {
            // 1. 설문 존재 여부 및 소유자 확인
            Optional<Survey> surveyOpt = surveyRepository.findById(surveyId);
            if (!surveyOpt.isPresent() || surveyOpt.get().getIsDeleted()) {
                return BaseResponse.fail(ResponseMessage.SURVEY_NOT_FOUND);
            }
            
            Survey survey = surveyOpt.get();
            Long currentUserId = userRepository.findIdByUsername(userDetail.getUsername());
            if (!survey.getUserId().equals(currentUserId)) {
                return BaseResponse.fail(ResponseMessage.UNAUTHORIZED);
            }
            
            // 2. 질문 존재 여부 확인
            Optional<Question> questionOpt = questionRepository.findById(questionId);
            if (!questionOpt.isPresent()) {
                return BaseResponse.fail(ResponseMessage.QUESTION_NOT_FOUND);
            }
            
            Question question = questionOpt.get();
            
            // 3. 질문이 해당 설문에 속하는지 확인
            if (!question.getSurveyId().equals(surveyId)) {
                return BaseResponse.fail(ResponseMessage.QUESTION_NOT_FOUND);
            }
            
            // 4. 통계 데이터 조회
            QuestionStatisticsResponseDto statistics = statisticsService.getQuestionStatisticsResponse(questionId);
            
            // 5. 인사이트 조회 (선택적, 실패해도 통계는 반환)
            try {
                String insight = insightService.getInsight(questionId);
                if (insight != null && !insight.isEmpty()) {
                    statistics.setInsight(insight);
                }
            } catch (Exception e) {
                log.warn("인사이트 조회 실패 (통계는 정상 반환): questionId={}, error={}", questionId, e.getMessage());
                // 인사이트가 없어도 통계는 반환
            }
            
            return BaseResponse.success(statistics);
            
        } catch (IllegalArgumentException e) {
            log.error("질문 통계 조회 실패: questionId={}, error={}", questionId, e.getMessage());
            return BaseResponse.fail(ResponseMessage.QUESTION_NOT_FOUND);
        } catch (Exception e) {
            log.error("질문 통계 조회 중 오류 발생: questionId={}", questionId, e);
            return BaseResponse.fail(ResponseMessage.QUESTION_STATISTICS_ERROR);
        }
    }

    /**
     * 질문별 전체 응답 조회
     * 설문 소유자만 조회 가능
     * 페이징 지원
     *
     * @param surveyId 설문 ID
     * @param questionId 질문 ID
     * @param page 페이지 번호 (기본값: 1)
     * @param size 페이지당 응답 수 (기본값: 10, 최대: 100)
     * @param userDetail 현재 사용자 정보
     * @return 질문별 응답 데이터 (페이징 정보 포함)
     */
    @GetMapping("/{surveyId}/questions/{questionId}/responses")
    @PreAuthorize("hasRole('CREATOR')")
    public BaseResponse<QuestionResponsesResponseDto> getQuestionResponses(
            @PathVariable Long surveyId,
            @PathVariable Long questionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetailImpl userDetail) {
        
        try {
            // 1. 페이징 파라미터 검증
            if (page < 1) {
                return BaseResponse.fail(ResponseMessage.INVALID_PAGE_NUMBER);
            }
            if (size < 1 || size > 100) {
                return BaseResponse.fail(ResponseMessage.INVALID_PAGE_SIZE);
            }
            
            // 2. 설문 존재 여부 및 소유자 확인
            Optional<Survey> surveyOpt = surveyRepository.findById(surveyId);
            if (!surveyOpt.isPresent() || surveyOpt.get().getIsDeleted()) {
                return BaseResponse.fail(ResponseMessage.SURVEY_NOT_FOUND);
            }
            
            Survey survey = surveyOpt.get();
            Long currentUserId = userRepository.findIdByUsername(userDetail.getUsername());
            if (!survey.getUserId().equals(currentUserId)) {
                return BaseResponse.fail(ResponseMessage.UNAUTHORIZED);
            }
            
            // 3. 질문 존재 여부 확인
            Optional<Question> questionOpt = questionRepository.findById(questionId);
            if (!questionOpt.isPresent()) {
                return BaseResponse.fail(ResponseMessage.QUESTION_NOT_FOUND);
            }
            
            Question question = questionOpt.get();
            
            // 4. 질문이 해당 설문에 속하는지 확인
            if (!question.getSurveyId().equals(surveyId)) {
                return BaseResponse.fail(ResponseMessage.QUESTION_NOT_FOUND);
            }
            
            // 5. 응답 조회
            QuestionResponsesResponseDto responses = responseService.getQuestionResponses(questionId, page, size);
            
            return BaseResponse.success(responses);
            
        } catch (IllegalArgumentException e) {
            log.error("질문 응답 조회 실패: questionId={}, error={}", questionId, e.getMessage());
            return BaseResponse.fail(ResponseMessage.QUESTION_NOT_FOUND);
        } catch (Exception e) {
            log.error("질문 응답 조회 중 오류 발생: questionId={}", questionId, e);
            return BaseResponse.fail(ResponseMessage.RESPONSE_FETCH_ERROR);
        }
    }
}
