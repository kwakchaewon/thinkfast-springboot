package com.example.thinkfast.service.survey;

import com.example.thinkfast.common.utils.HashUtil;
import com.example.thinkfast.domain.survey.Option;
import com.example.thinkfast.domain.survey.Question;
import com.example.thinkfast.domain.survey.Survey;
import com.example.thinkfast.dto.survey.CreateSurveyRequest;
import com.example.thinkfast.dto.survey.GetRecentSurveysResponse;
import com.example.thinkfast.dto.survey.GetSurveyDetailResponse;
import com.example.thinkfast.dto.survey.PaginationDto;
import com.example.thinkfast.dto.survey.PublicSurveyDto;
import com.example.thinkfast.dto.survey.PublicSurveyListResponse;
import com.example.thinkfast.realtime.RedisPublisher;
import com.example.thinkfast.repository.ai.InsightReportRepository;
import com.example.thinkfast.repository.survey.OptionRepository;
import com.example.thinkfast.repository.survey.QuestionRepository;
import com.example.thinkfast.repository.survey.SurveyRepository;
import com.example.thinkfast.repository.survey.SurveyResponseHistoryRepository;
import com.example.thinkfast.repository.auth.UserRepository;
import com.example.thinkfast.security.UserDetailImpl;
import com.example.thinkfast.service.ai.InsightService;
import com.example.thinkfast.service.ai.SummaryService;
import com.example.thinkfast.service.ai.WordCloudService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SurveyService {
    private final UserRepository userRepository;
    private final OptionRepository optionRepository;
    private final QuestionRepository questionRepository;
    private final SurveyRepository surveyRepository;
    private final SurveyResponseHistoryRepository surveyResponseHistoryRepository;
    private final SummaryService summaryService;
    private final WordCloudService wordCloudService;
    private final InsightService insightService;
    private final InsightReportRepository insightReportRepository;
    private final RedisPublisher redisPublisher;

    @Transactional
    public void createSurvey(UserDetailImpl userDetail, CreateSurveyRequest createSurveyRequest) {
        Long userId = userRepository.findIdByUsername(userDetail.getUsername());

        // 1. survey 테이블 저장
        Survey survey = Survey.builder()
                .userId(userId)
                .title(createSurveyRequest.getTitle())
                .description(createSurveyRequest.getDescription())
                .endTime(LocalDateTime.of(createSurveyRequest.getEndDate(), createSurveyRequest.getEndTime()))
                .isDeleted(false)
                .showResults(createSurveyRequest.isShowResults())
                .build();
        Survey createdSurvey = surveyRepository.save(survey);

        // 2. question 테이블 저장
        // required 필드 현재 없음
        // 우선 MULTIPLE_CHOICE(객관식), SUBJECTIVE(주관식) 만 구현
        for (CreateSurveyRequest.QuestionRequest questionRequest: createSurveyRequest.getQuestions()){
            Question question = Question.builder()
                    .surveyId(createdSurvey.getId())
                    .type(Question.QuestionType.valueOf(questionRequest.getType()))
                    .content(questionRequest.getContent())
                    .orderIndex(questionRequest.getOrderIndex())
                    .build();
            Question createdQuestion = questionRepository.save(question);

            // 객관식일 경우
            // 3. Option 테이블 저장
            if (questionRequest.getType().equals("MULTIPLE_CHOICE")){
                for(String optionRequest: questionRequest.getOptions()){
                    Option option = Option.builder()
                            .questionId(createdQuestion.getId())
                            .content(optionRequest)
                            .build();

                    optionRepository.save(option);
                }
            }
        }
    }

    @Transactional
    public void deleteSurvey(Long id){
        Survey survey = surveyRepository.findById(id).get();
        survey.setIsDeleted(true);
        surveyRepository.save(survey);
    }

    @Transactional(readOnly = true)
    public List<GetRecentSurveysResponse> getSurveys(UserDetailImpl userDetail) {
        Long userId = userRepository.findIdByUsername(userDetail.getUsername());
        return surveyRepository.getRecentSurveys(userId);
    }

    @Transactional(readOnly = true)
    public List<GetRecentSurveysResponse> getRecentSurveys(UserDetailImpl userDetail) {
        Long userId = userRepository.findIdByUsername(userDetail.getUsername());
        List<GetRecentSurveysResponse> surveys = surveyRepository.getRecentSurveys(userId);

        // 상위 5개 row 까지만 return
        return surveys.subList(0, Math.min(5, surveys.size()));
    }

    @Transactional(readOnly = true)
    public GetSurveyDetailResponse getSurveyDetail(Long id) {
        return surveyRepository.findByIdAndIsDeletedFalse(id);
    }

    @Transactional(readOnly = true)
    public Boolean isSurveyInactive(Long id){
        return surveyRepository.chekcIsInactiveOrDeleted(id ,true, false);
    }

    public Boolean isDuplicateResponse(Long surveyId, String deviceId, String ipAddress){
        log.info("[중복 응답 체크 시작] surveyId={}, deviceId={}, ipAddress={}", 
            surveyId, 
            deviceId != null ? (deviceId.length() > 20 ? deviceId.substring(0, 20) + "..." : deviceId) : "null",
            ipAddress != null ? ipAddress : "null");
        
        // deviceId와 IP를 모두 사용하여 중복 체크 (둘 다 같아야 중복으로 판단)
        // 이렇게 하면 같은 브라우저(User-Agent 기반 deviceId)를 사용하더라도 다른 IP에서는 응답 가능
        String deviceIdHash = null;
        String ipAddressHash = null;
        boolean isDuplicate = false;
        
        // deviceId와 IP가 모두 존재하는 경우에만 조합으로 체크
        if (deviceId != null && !deviceId.trim().isEmpty() && 
            ipAddress != null && !ipAddress.trim().isEmpty()) {
            deviceIdHash = HashUtil.encodeSha256(deviceId);
            ipAddressHash = HashUtil.encodeSha256(ipAddress);
            isDuplicate = surveyResponseHistoryRepository.existsBySurveyIdAndDeviceIdHashAndIpAddressHash(
                surveyId, deviceIdHash, ipAddressHash);
            log.info("[중복 응답 체크 - DeviceId+IP 조합] surveyId={}, deviceIdHash={}, ipAddressHash={}, 중복여부={}", 
                surveyId, deviceIdHash, ipAddressHash, isDuplicate);
        } 
        // deviceId만 있는 경우 (IP가 없는 경우)
        else if (deviceId != null && !deviceId.trim().isEmpty()) {
            deviceIdHash = HashUtil.encodeSha256(deviceId);
            isDuplicate = surveyResponseHistoryRepository.existsBySurveyIdAndDeviceIdHash(surveyId, deviceIdHash);
            log.info("[중복 응답 체크 - DeviceId만] surveyId={}, deviceIdHash={}, 중복여부={}", 
                surveyId, deviceIdHash, isDuplicate);
            log.warn("[중복 응답 체크] surveyId={}, IP가 없어서 DeviceId만으로 체크", surveyId);
        }
        // IP만 있는 경우 (deviceId가 없는 경우)
        else if (ipAddress != null && !ipAddress.trim().isEmpty()) {
            ipAddressHash = HashUtil.encodeSha256(ipAddress);
            isDuplicate = surveyResponseHistoryRepository.existsBySurveyIdAndIpAddressHash(surveyId, ipAddressHash);
            log.info("[중복 응답 체크 - IP만] surveyId={}, ipAddressHash={}, 중복여부={}", 
                surveyId, ipAddressHash, isDuplicate);
            log.warn("[중복 응답 체크] surveyId={}, deviceId가 없어서 IP만으로 체크", surveyId);
        }
        // 둘 다 없는 경우
        else {
            log.warn("[중복 응답 체크] surveyId={}, deviceId와 IP가 모두 없어서 중복 체크 불가 (허용)", surveyId);
            isDuplicate = false;
        }

        log.info("[중복 응답 체크 결과] surveyId={}, 최종결과={}", surveyId, isDuplicate);
        return isDuplicate;
    }

    /**
     * 공개 설문 목록 조회 (페이징 및 정렬 지원)
     *
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지당 설문 수
     * @param sort 정렬 기준 (newest, oldest, responses)
     * @param search 검색 키워드
     * @return 공개 설문 목록 및 페이징 정보
     */
    @Transactional(readOnly = true)
    public PublicSurveyListResponse getPublicSurveys(int page, int size, String sort, String search) {
        // 1. 페이징 파라미터 보정
        if (page < 1) {
            page = 1;
        }
        if (size < 1 || size > 100) {
            size = 10; // 기본값
        }

        // 2. 정렬 기준 설정
        Sort sortObj;
        if ("oldest".equalsIgnoreCase(sort)) {
            sortObj = Sort.by(Sort.Direction.ASC, "createdAt");
        } else if ("responses".equalsIgnoreCase(sort)) {
            // responses 정렬은 쿼리에서 ORDER BY 처리하므로 Pageable에는 정렬 미적용
            sortObj = Sort.unsorted();
        } else {
            // 기본값: newest (최신순)
            sortObj = Sort.by(Sort.Direction.DESC, "createdAt");
        }

        // 3. 페이징 객체 생성 (Spring Data는 0부터 시작)
        Pageable pageable = PageRequest.of(page - 1, size, sortObj);

        // 4. 공개 설문 조회
        Page<PublicSurveyDto> surveyPage;
        if ("responses".equalsIgnoreCase(sort)) {
            surveyPage = surveyRepository.findPublicSurveysOrderByResponses(search, pageable);
        } else {
            surveyPage = surveyRepository.findPublicSurveys(search, pageable);
        }

        // 5. 페이징 정보 생성
        PaginationDto pagination = new PaginationDto(
                page,
                size,
                surveyPage.getTotalPages(),
                surveyPage.getTotalElements()
        );

        // 6. 응답 DTO 생성
        return new PublicSurveyListResponse(surveyPage.getContent(), pagination);
    }

    /**
     * 특정 설문이 종료되었는지 확인하고, 종료된 경우 AI 리포트들을 업데이트
     * 응답 등록 시 호출되어 설문 종료 후 즉시 리포트가 업데이트되도록 함
     *
     * @param surveyId 설문 ID
     */
    @Transactional
    public void checkAndUpdateExpiredSurveyReports(Long surveyId) {
        try {
            Optional<Survey> surveyOpt = surveyRepository.findById(surveyId);
            if (!surveyOpt.isPresent()) {
                log.warn("[설문 종료 체크] 설문을 찾을 수 없음: surveyId={}", surveyId);
                return;
            }

            Survey survey = surveyOpt.get();
            LocalDateTime now = LocalDateTime.now();

            // 설문이 활성화되어 있고 종료 시간이 지났는지 확인
            if (survey.getIsActive() && survey.getEndTime() != null && survey.getEndTime().isBefore(now)) {
                log.info("[설문 종료 감지] surveyId={}, 제목={}, 종료일={}", 
                    surveyId, survey.getTitle(), survey.getEndTime().toLocalDate());

                // 설문 비활성화 처리
                survey.setIsActive(false);
                surveyRepository.save(survey);

                // Redis 알림 전송
                redisPublisher.sendAlarm(surveyId, "SURVEY_EXPIRED");

                // 설문 종료 후 AI 리포트들 업데이트 (비동기 처리)
                updateSurveyReports(surveyId);
            }
        } catch (Exception e) {
            log.error("[설문 종료 체크 실패] surveyId={}", surveyId, e);
        }
    }

    /**
     * 설문 종료 후 AI 리포트들 업데이트
     * Summary, Insight, Statistics, WordCloud를 모두 업데이트
     *
     * @param surveyId 설문 ID
     */
    private void updateSurveyReports(Long surveyId) {
        try {
            log.info("[AI 리포트 업데이트 시작] surveyId={}", surveyId);

            // 설문 종료 후 요약 리포트 업데이트 (비동기)
            summaryService.saveSummaryReportAsync(surveyId);

            // 설문 종료 후 워드클라우드 업데이트 (비동기)
            wordCloudService.saveWordCloudsForSurveyAsync(surveyId);

            // 설문 종료 후 인사이트 텍스트 업데이트 (비동기)
            insightService.saveInsightsForSurveyAsync(surveyId);

            // Statistics는 실시간 계산되므로 별도 업데이트 불필요
            // 필요시 통계 캐시 무효화 로직을 여기에 추가할 수 있음

            log.info("[AI 리포트 업데이트 요청 완료] surveyId={}", surveyId);
        } catch (Exception e) {
            log.error("[AI 리포트 업데이트 실패] surveyId={}", surveyId, e);
        }
    }
}
