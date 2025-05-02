package com.example.thinkfast.service.survey;

import com.example.thinkfast.domain.survey.Response;
import com.example.thinkfast.dto.survey.CreateResponseRequest;
import com.example.thinkfast.repository.survey.ResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResponseService {
    private final ResponseRepository responseRepository;

    @Transactional
    public void createResponse(CreateResponseRequest createResponseRequest){
        for (CreateResponseRequest.CreateResponseDto createResponseDto : createResponseRequest.getAnswers()){
            Response response = Response.builder()
                    .questionId(createResponseDto.getQuestionId())
                    .optionId(createResponseDto.getOptionId())
                    .subjectiveContent(createResponseDto.getContent())
                    .questionType(createResponseDto.getType().toString())
                    .build();
            Response result =  responseRepository.save(response);
        }
    }

}
