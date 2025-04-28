package com.example.thinkfast.service.survey;

import com.example.thinkfast.domain.survey.Answer;
import com.example.thinkfast.dto.survey.CreateAnswerRequest;
import com.example.thinkfast.repository.survey.AnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnswerService {
    private final AnswerRepository answerRepository;

    @Transactional
    public void createAnswer(CreateAnswerRequest createAnswerRequest){
        for (CreateAnswerRequest.CreateAnswerDto createAnswerDto : createAnswerRequest.getAnswers()){
            Answer answer = Answer.builder()
                    .questionId(createAnswerDto.getQuestionId())
                    .optionId(createAnswerDto.getOptionId())
                    .subjectiveContent(createAnswerDto.getContent())
                    .questionType(createAnswerDto.getType().toString())
                    .build();
            Answer result =  answerRepository.save(answer);
        }
    }

}
