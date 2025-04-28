package com.example.thinkfast.repository.survey;

import com.example.thinkfast.domain.survey.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
}
