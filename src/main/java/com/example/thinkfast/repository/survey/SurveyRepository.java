package com.example.thinkfast.repository.survey;

import com.example.thinkfast.domain.survey.Survey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SurveyRepository extends JpaRepository<Survey, Long> {
    // 기본 CRUD 메서드가 자동으로 제공됩니다:
    // - save(Survey entity)
    // - findById(Long id)
    // - findAll()
    // - deleteById(Long id)
    // - delete(Survey entity)
    // - count()
    // - existsById(Long id)
} 