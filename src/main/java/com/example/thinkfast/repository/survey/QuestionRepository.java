package com.example.thinkfast.repository.survey;

import com.example.thinkfast.domain.survey.Question;
import com.example.thinkfast.dto.survey.QuestionDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    // 기본 CRUD 메서드가 자동으로 제공됩니다:
    // - save(Question entity)
    // - findById(Long id)
    // - findAll()
    // - deleteById(Long id)
    // - delete(Question entity)
    // - count()
    // - existsById(Long id)

    List<Question> findBySurveyId(Long surveyId);

    @Query("SELECT new com.example.thinkfast.dto.survey.QuestionDto(" +
           "q.id, q.surveyId, q.type, q.content, q.orderIndex) " +
           "FROM Question q " +
           "WHERE q.id = :questionId")
    QuestionDto findQuestionById(@Param("questionId") Long questionId);
}