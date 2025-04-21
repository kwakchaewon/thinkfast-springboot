package com.example.thinkfast.service;

import com.example.thinkfast.domain.survey.Option;
import com.example.thinkfast.domain.survey.Question;
import com.example.thinkfast.domain.survey.Survey;
import com.example.thinkfast.dto.survey.CreateSurveyRequest;
import com.example.thinkfast.dto.survey.GetRecentSuveysResponse;
import com.example.thinkfast.repository.auth.UserRepository;
import com.example.thinkfast.repository.survey.OptionRepository;
import com.example.thinkfast.repository.survey.QuestionRepository;
import com.example.thinkfast.repository.survey.SurveyRepository;
import com.example.thinkfast.security.UserDetailImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SurveyService {
    private final UserRepository userRepository;
    private final OptionRepository optionRepository;
    private final QuestionRepository questionRepository;
    private final SurveyRepository surveyRepository;

    @Transactional
    public void createSurvey(UserDetailImpl userDetail, CreateSurveyRequest createSurveyRequest) {
        Long creatorId = userRepository.findIdByUsername(userDetail.getUsername());

        // 1. survey 테이블 저장
        // show result 필드 현재 없음
        Survey survey = Survey.builder()
                .creatorId(creatorId)
                .title(createSurveyRequest.getTitle())
                .description(createSurveyRequest.getDescription())
                .endTime(LocalDateTime.of(createSurveyRequest.getEndDate(), createSurveyRequest.getEndTime()))
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

    @Transactional(readOnly = true)
    public List<GetRecentSuveysResponse> getRecentSurveys(UserDetailImpl userDetail) {
        Long creatorId = userRepository.findIdByUsername(userDetail.getUsername());
        List<GetRecentSuveysResponse> surveys = surveyRepository.findTop5GetRecentSuveysResponseByCreatorIdOrderByCreatedAtDesc(creatorId);
        return surveys;
    }
}
