package com.example.thinkfast.repository.ai;

import com.example.thinkfast.domain.ai.WordCloud;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WordCloudRepository extends JpaRepository<WordCloud, Long> {
    /**
     * 질문 ID로 워드클라우드 조회
     */
    Optional<WordCloud> findByQuestionId(Long questionId);

    /**
     * 질문 ID로 워드클라우드 존재 여부 확인
     */
    boolean existsByQuestionId(Long questionId);
}

