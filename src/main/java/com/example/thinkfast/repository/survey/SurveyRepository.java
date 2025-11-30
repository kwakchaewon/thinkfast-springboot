package com.example.thinkfast.repository.survey;

import com.example.thinkfast.domain.survey.Question;
import com.example.thinkfast.domain.survey.Survey;
import com.example.thinkfast.dto.survey.GetRecentSurveysResponse;
import com.example.thinkfast.dto.survey.GetSurveyDetailResponse;
import com.example.thinkfast.dto.survey.PublicSurveyDto;
import org.springframework.data.domain.Page;
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

    /**
     * 진행 중인 설문 조회 (활성화되어 있고 종료 시간이 아직 지나지 않은 설문)
     * - isActive = true
     * - isDeleted = false
     * - endTime > 현재 시간
     */
    @Query("SELECT s FROM Survey s " +
           "WHERE s.isActive = true " +
           "AND s.isDeleted = false " +
           "AND s.endTime > :now")
    List<Survey> findActiveSurveysByEndTimeAfter(@Param("now") LocalDateTime now);

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

    /**
     * 공개 설문 목록 조회 (기본 정렬: createdAt 기준)
     * - showResults = true
     * - isDeleted = false
     * - search: 제목, 설명, 작성자명 부분 검색
     */
    @Query("SELECT new com.example.thinkfast.dto.survey.PublicSurveyDto(" +
            "s.id, s.title, s.description, s.isActive, s.endTime, " +
            "COUNT(DISTINCT r.responseSessionId), s.createdAt, s.showResults, " +
            "s.userId, u.realUsername) " +
            "FROM Survey s " +
            "JOIN User u ON s.userId = u.id " +
            "LEFT JOIN Question q on s.id = q.surveyId " +
            "LEFT JOIN Response r ON q.id = r.questionId " +
            "WHERE s.showResults = true AND s.isDeleted = false " +
            "AND (:search IS NULL OR :search = '' OR " +
            "LOWER(s.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.realUsername) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "GROUP BY s.id, s.title, s.description, s.isActive, s.endTime, " +
            "s.createdAt, s.showResults, s.userId, u.realUsername")
    Page<PublicSurveyDto> findPublicSurveys(
            @Param("search") String search,
            Pageable pageable);

    /**
     * 공개 설문 목록 조회 - 응답 수 기준 내림차순 정렬
     */
    @Query("SELECT new com.example.thinkfast.dto.survey.PublicSurveyDto(" +
            "s.id, s.title, s.description, s.isActive, s.endTime, " +
            "COUNT(DISTINCT r.responseSessionId), s.createdAt, s.showResults, " +
            "s.userId, u.realUsername) " +
            "FROM Survey s " +
            "JOIN User u ON s.userId = u.id " +
            "LEFT JOIN Question q on s.id = q.surveyId " +
            "LEFT JOIN Response r ON q.id = r.questionId " +
            "WHERE s.showResults = true AND s.isDeleted = false " +
            "AND (:search IS NULL OR :search = '' OR " +
            "LOWER(s.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.realUsername) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "GROUP BY s.id, s.title, s.description, s.isActive, s.endTime, " +
            "s.createdAt, s.showResults, s.userId, u.realUsername " +
            "ORDER BY COUNT(DISTINCT r.responseSessionId) DESC, s.createdAt DESC")
    Page<PublicSurveyDto> findPublicSurveysOrderByResponses(
            @Param("search") String search,
            Pageable pageable);
}
