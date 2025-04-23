package com.example.thinkfast.service.survey;

import com.example.thinkfast.domain.survey.Option;
import com.example.thinkfast.domain.survey.Question;
import com.example.thinkfast.dto.survey.QuestionDto;
import com.example.thinkfast.repository.survey.OptionRepository;
import com.example.thinkfast.repository.survey.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionService {
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;

    @Transactional(readOnly = true)
    public QuestionDto getQuestionWithOptions(Long questionId) {
        QuestionDto question = questionRepository.findQuestionById(questionId);

        if (question != null) {
            question.setOptions(optionRepository.findOptionsByQuestionId(questionId));
        }
        return question;
    }

    @Transactional(readOnly = true)
    public List<Question> getIdsBySurveyId(Long id){
        return questionRepository.findBySurveyId(id);
    }
} 