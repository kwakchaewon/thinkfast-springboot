package com.example.thinkfast.repository.survey;

import com.example.thinkfast.domain.survey.Option;
import com.example.thinkfast.dto.survey.OptionDto;
import com.example.thinkfast.dto.survey.QuestionDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OptionRepository extends JpaRepository<Option, Long> {
    // - save(Option entity)
    // - findById(Long id)
    // - findAll()
    // - deleteById(Long id)
    // - delete(Option entity)
    // - count()
    // - existsById(Long id)
    @Query("SELECT new com.example.thinkfast.dto.survey.OptionDto(o.id, o.content) " +
            "FROM Option o " +
            "WHERE o.questionId = :questionId " +
            "ORDER BY o.id ASC")
    List<OptionDto> findOptionsByQuestionId(@Param("questionId") Long questionId);
    
    /**
     * 질문 ID로 모든 옵션 조회 (엔티티 반환)
     */
    List<Option> findByQuestionIdOrderByIdAsc(Long questionId);
}