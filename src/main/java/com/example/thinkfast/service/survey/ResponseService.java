package com.example.thinkfast.service.survey;

import com.example.thinkfast.domain.survey.Response;
import com.example.thinkfast.dto.survey.CreateResponseRequest;
import com.example.thinkfast.repository.auth.UserRepository;
import com.example.thinkfast.repository.survey.ResponseRepository;
import com.example.thinkfast.security.UserDetailImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResponseService {
    private final ResponseRepository responseRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createResponse(UserDetailImpl userDetail , CreateResponseRequest createResponseRequest){
        Long userId = userRepository.findIdByUsername(userDetail.getUsername());
        for (CreateResponseRequest.CreateResponseDto createResponseDto : createResponseRequest.getAnswers()){
            Response response = Response.builder()
                    .userId(userId)
                    .questionId(createResponseDto.getQuestionId())
                    .optionId(createResponseDto.getOptionId())
                    .subjectiveContent(createResponseDto.getContent())
                    .questionType(createResponseDto.getType().toString())
                    .build();
            Response result =  responseRepository.save(response);
        }
    }

}
