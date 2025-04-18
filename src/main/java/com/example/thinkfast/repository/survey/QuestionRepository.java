package com.example.thinkfast.repository.survey;

import com.example.thinkfast.domain.survey.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
} 