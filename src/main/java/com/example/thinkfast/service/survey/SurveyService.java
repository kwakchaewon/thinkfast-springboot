package com.example.thinkfast.service.survey;

import com.example.thinkfast.common.utils.HashUtil;
import com.example.thinkfast.domain.survey.Option;
import com.example.thinkfast.domain.survey.Question;
import com.example.thinkfast.domain.survey.Survey;
import com.example.thinkfast.dto.survey.CreateSurveyRequest;
import com.example.thinkfast.dto.survey.GetRecentSurveysResponse;
import com.example.thinkfast.dto.survey.GetSurveyDetailResponse;
import com.example.thinkfast.repository.auth.UserRepository;
import com.example.thinkfast.repository.survey.OptionRepository;
import com.example.thinkfast.repository.survey.QuestionRepository;
import com.example.thinkfast.repository.survey.SurveyRepository;
import com.example.thinkfast.repository.survey.SurveyResponseHistoryRepository;
import com.example.thinkfast.security.UserDetailImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SurveyService {
    private final UserRepository userRepository;
    private final OptionRepository optionRepository;
    private final QuestionRepository questionRepository;
    private final SurveyRepository surveyRepository;
    private final SurveyResponseHistoryRepository surveyResponseHistoryRepository;

    @Transactional
    public void createSurvey(UserDetailImpl userDetail, CreateSurveyRequest createSurveyRequest) {
        Long userId = userRepository.findIdByUsername(userDetail.getUsername());

        // 1. survey 테이블 저장
        // show result 필드 현재 없음
        Survey survey = Survey.builder()
                .userId(userId)
                .title(createSurveyRequest.getTitle())
                .description(createSurveyRequest.getDescription())
                .endTime(LocalDateTime.of(createSurveyRequest.getEndDate(), createSurveyRequest.getEndTime()))
                .isDeleted(false)
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
        
        // deviceId나 ipAddress가 null이거나 빈 값이면 중복 체크를 건너뜀
        // (null/빈 값은 항상 같은 해시값을 생성하므로 중복으로 잘못 판단될 수 있음)
        boolean byDevice = false;
        boolean byIp = false;
        String deviceIdHash = null;
        String ipAddressHash = null;
        
        if (deviceId != null && !deviceId.trim().isEmpty()) {
            deviceIdHash = HashUtil.encodeSha256(deviceId);
            byDevice = surveyResponseHistoryRepository.existsBySurveyIdAndDeviceIdHash(surveyId, deviceIdHash);
            log.info("[중복 응답 체크 - DeviceId] surveyId={}, deviceIdHash={}, 중복여부={}", 
                surveyId, deviceIdHash, byDevice);
        } else {
            log.warn("[중복 응답 체크 - DeviceId] surveyId={}, deviceId가 null이거나 빈 값이어서 체크 건너뜀", surveyId);
        }
        
        if (ipAddress != null && !ipAddress.trim().isEmpty()) {
            ipAddressHash = HashUtil.encodeSha256(ipAddress);
            byIp = surveyResponseHistoryRepository.existsBySurveyIdAndIpAddressHash(surveyId, ipAddressHash);
            log.info("[중복 응답 체크 - IP] surveyId={}, ipAddressHash={}, 중복여부={}", 
                surveyId, ipAddressHash, byIp);
        } else {
            log.warn("[중복 응답 체크 - IP] surveyId={}, ipAddress가 null이거나 빈 값이어서 체크 건너뜀", surveyId);
        }

        Boolean result = byDevice || byIp;
        log.info("[중복 응답 체크 결과] surveyId={}, 최종결과={} (byDevice={}, byIp={})", 
            surveyId, result, byDevice, byIp);
        return result;
    }
}
