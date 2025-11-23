package com.example.thinkfast.service.survey;

import com.example.thinkfast.common.utils.HashUtil;
import com.example.thinkfast.domain.survey.Response;
import com.example.thinkfast.domain.survey.SurveyResponseHistory;
import com.example.thinkfast.dto.survey.CreateResponseRequest;
import com.example.thinkfast.repository.auth.UserRepository;
import com.example.thinkfast.repository.survey.ResponseRepository;
import com.example.thinkfast.repository.survey.SurveyResponseHistoryRepository;
import com.example.thinkfast.security.UserDetailImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResponseService {
    private final ResponseRepository responseRepository;
    private final UserRepository userRepository;
    private final SurveyResponseHistoryRepository surveyResponseHistoryRepository;

    @Transactional
    public void createResponse(UserDetailImpl userDetail, Long surveyId, String ipAddress, CreateResponseRequest createResponseRequest){
        String responseSessionId = getRandomUuid();

        for (CreateResponseRequest.CreateResponseDto createResponseDto : createResponseRequest.getAnswers()){
            Response response = Response.builder()
                    .responseSessionId(responseSessionId)
                    .questionId(createResponseDto.getQuestionId())
                    .optionId(createResponseDto.getOptionId())
                    .subjectiveContent(createResponseDto.getContent())
                    .questionType(createResponseDto.getType().toString())
                    .build();
            Response result =  responseRepository.save(response);
        }

        String deviceId = createResponseRequest.getClientInfo() != null 
            ? createResponseRequest.getClientInfo().getDeviceId() 
            : null;
        this.createSurveyResponseHistory(surveyId, ipAddress, deviceId);
    }

    public void createSurveyResponseHistory(Long surveyId, String ipAddress, String deviceId){
        log.info("[응답 이력 저장 시작] surveyId={}, deviceId={}, ipAddress={}", 
            surveyId,
            deviceId != null ? (deviceId.length() > 20 ? deviceId.substring(0, 20) + "..." : deviceId) : "null",
            ipAddress != null ? ipAddress : "null");
        
        // deviceId가 null이거나 빈 값이면 기본값 사용 (DEVICE_ID_HASH는 nullable=false이므로)
        // 단, 기본값을 사용하면 중복 체크에서 제외됨 (SurveyService에서 null 체크)
        boolean isDeviceIdEmpty = (deviceId == null || deviceId.trim().isEmpty());
        String deviceIdHash;
        if (isDeviceIdEmpty) {
            String defaultDeviceId = "UNKNOWN_DEVICE_" + surveyId + "_" + System.currentTimeMillis();
            deviceIdHash = HashUtil.encodeSha256(defaultDeviceId);
            log.warn("[응답 이력 저장] surveyId={}, deviceId가 null/빈 값이어서 기본값 사용: {}", surveyId, defaultDeviceId);
        } else {
            deviceIdHash = HashUtil.encodeSha256(deviceId);
        }
        
        // ipAddress는 nullable이므로 null 허용
        boolean isIpAddressEmpty = (ipAddress == null || ipAddress.trim().isEmpty());
        String ipAddressHash;
        if (isIpAddressEmpty) {
            ipAddressHash = null;
            log.warn("[응답 이력 저장] surveyId={}, ipAddress가 null/빈 값이어서 null로 저장", surveyId);
        } else {
            ipAddressHash = HashUtil.encodeSha256(ipAddress);
        }

        SurveyResponseHistory surveyResponseHistory = SurveyResponseHistory.builder()
                .surveyId(surveyId)
                .deviceIdHash(deviceIdHash)
                .ipAddressHash(ipAddressHash)
                .build();
        surveyResponseHistoryRepository.save(surveyResponseHistory);
        
        log.info("[응답 이력 저장 완료] surveyId={}, deviceIdHash={}, ipAddressHash={}", 
            surveyId, deviceIdHash, ipAddressHash != null ? ipAddressHash : "null");
    }

    public String getRandomUuid(){
        return UUID.randomUUID().toString();
    }

}
