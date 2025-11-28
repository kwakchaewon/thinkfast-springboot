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
            "s.id, s.title, s.description, s.startTime, s.isActive, s.createdAt, " +
            "COUNT(DISTINCT r.responseSessionId)) " +
            "FROM Survey s " +
            "LEFT JOIN Question q on s.id = q.surveyId " +
            "LEFT JOIN Response r ON q.id = r.questionId " +
            "WHERE s.userId = :userId AND s.isDeleted = false AND s.showResults = true " +
            "GROUP BY s.id, s.title, s.description, s.startTime, s.isActive, s.createdAt " +
            "ORDER BY s.createdAt DESC")
    List<GetRecentSurveysResponse> getRecentSurveys(@Param("userId") Long userId);
    GetSurveyDetailResponse findByIdAndIsDeletedFalse(Long id);
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
            "FROM Survey s " +
            "WHERE s.id = :id AND (s.isDeleted = :isDeleted OR s.isActive = :isActive)")
    Boolean chekcIsInactiveOrDeleted(
            @Param("id") Long id,
            @Param("isDeleted") Boolean isDeleted,
            @Param("isActive") Boolean isActive
    );
    @Query("SELECT s.userId FROM Survey s WHERE s.id = :id")
    Long findUserIdById(@Param("id") Long id);
}