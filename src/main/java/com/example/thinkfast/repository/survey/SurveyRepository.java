package com.example.thinkfast.repository.survey;

import com.example.thinkfast.domain.survey.Survey;
import com.example.thinkfast.dto.survey.GetRecentSurveysResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

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

    // 활성화된 설문 조회
    List<Survey> findByIsActiveTrue();

    // 종료 시간이 지난 활성화된 설문 조회
    List<Survey> findByIsActiveTrueAndEndTimeBefore(LocalDateTime endDate);

    List<GetRecentSurveysResponse> findGetRecentSurveysResponseByCreatorIdOrderByCreatedAtDesc(Long creatorId);
} 