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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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

        this.createSurveyResponseHistory(surveyId, ipAddress, createResponseRequest.getClientInfo().getDeviceId());
    }

    public void createSurveyResponseHistory(Long surveyId, String ipAddress, String DeviceId){
        String deviceIdHash = HashUtil.encodeSha256(DeviceId);
        String ipAddressHash = HashUtil.encodeSha256(ipAddress);

        SurveyResponseHistory surveyResponseHistory = SurveyResponseHistory.builder()
                .surveyId(surveyId)
                .deviceIdHash(deviceIdHash)
                .ipAddressHash(ipAddressHash)
                .build();
        surveyResponseHistoryRepository.save(surveyResponseHistory);
    }

    public String getRandomUuid(){
        return UUID.randomUUID().toString();
    }

}
