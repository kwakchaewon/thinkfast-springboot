package com.example.thinkfast.repository.ai;

import com.example.thinkfast.domain.ai.QuestionInsight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuestionInsightRepository extends JpaRepository<QuestionInsight, Long> {
    /**
     * 질문 ID로 인사이트 조회
     */
    Optional<QuestionInsight> findByQuestionId(Long questionId);

    /**
     * 질문 ID로 인사이트 존재 여부 확인
     */
    boolean existsByQuestionId(Long questionId);
}

