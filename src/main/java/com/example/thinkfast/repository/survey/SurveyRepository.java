package com.example.thinkfast.repository.survey;

import com.example.thinkfast.domain.survey.Question;
import com.example.thinkfast.domain.survey.Survey;
import com.example.thinkfast.dto.survey.GetRecentSurveysResponse;
import com.example.thinkfast.dto.survey.GetSurveyDetailResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    List<Survey> findByIsActiveTrue();

    List<Survey> findByIsActiveTrueAndEndTimeBefore(LocalDateTime endDate);

    @Query("SELECT new com.example.thinkfast.dto.survey.GetRecentSurveysResponse(" +
            "s.id, s.title, s.description, s.startTime, s.isActive, s.createdAt) " +
            "FROM Survey s " +
            "WHERE s.creatorId = :creatorId AND s.isDeleted = false " +
            "ORDER BY s.createdAt DESC")
    List<GetRecentSurveysResponse> getRecentSurveys(@Param("creatorId") Long creatorId);
    GetSurveyDetailResponse findByIdAndIsDeletedFalse(Long id);
    Boolean existsByIdAndIsDeletedOrIsActive(Long id, Boolean isDeleted, Boolean isActive);
}