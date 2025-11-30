package com.example.thinkfast.repository.survey;

import com.example.thinkfast.domain.survey.Response;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ResponseRepository extends JpaRepository<Response, Long> {
    
    /**
     * 질문별 모든 응답 조회
     */
    List<Response> findByQuestionId(Long questionId);
    
    /**
     * 객관식 질문의 옵션별 응답 수 집계
     */
    @Query("SELECT r.optionId, COUNT(r) as count " +
           "FROM Response r " +
           "WHERE r.questionId = :questionId " +
           "AND r.optionId IS NOT NULL " +
           "GROUP BY r.optionId")
    List<Object[]> countByQuestionIdAndOptionId(@Param("questionId") Long questionId);
    
    /**
     * 질문별 전체 응답 수 조회
     */
    @Query("SELECT COUNT(DISTINCT r.responseSessionId) " +
           "FROM Response r " +
           "WHERE r.questionId = :questionId")
    Long countDistinctResponseSessionsByQuestionId(@Param("questionId") Long questionId);
    
    /**
     * 설문의 모든 질문별 응답 조회
     */
    @Query("SELECT r FROM Response r " +
           "WHERE r.questionId IN " +
           "(SELECT q.id FROM Question q WHERE q.surveyId = :surveyId)")
    List<Response> findBySurveyId(@Param("surveyId") Long surveyId);
    
    /**
     * 질문별 응답 페이징 조회 (createdAt 내림차순)
     */
    @Query("SELECT r FROM Response r " +
           "WHERE r.questionId = :questionId " +
           "ORDER BY r.createdAt DESC, r.id DESC")
    List<Response> findByQuestionIdOrderByCreatedAtDesc(@Param("questionId") Long questionId, Pageable pageable);
    
    /**
     * 질문별 전체 응답 수 조회 (페이징용)
     */
    long countByQuestionId(Long questionId);

    /**
     * 설문의 전체 응답 수 조회 (중복 제거된 세션 수)
     */
    @Query("SELECT COUNT(DISTINCT r.responseSessionId) " +
           "FROM Response r " +
           "WHERE r.questionId IN " +
           "(SELECT q.id FROM Question q WHERE q.surveyId = :surveyId)")
    Long countDistinctResponseSessionsBySurveyId(@Param("surveyId") Long surveyId);

    /**
     * 여러 설문의 응답 수를 한 번에 조회 (배치 처리용)
     * 설문 ID와 응답 수를 매핑하여 반환
     *
     * @param surveyIds 설문 ID 리스트
     * @return [surveyId, responseCount] 형태의 Object 배열 리스트
     */
    @Query("SELECT q.surveyId, COUNT(DISTINCT r.responseSessionId) " +
           "FROM Question q " +
           "LEFT JOIN Response r ON q.id = r.questionId " +
           "WHERE q.surveyId IN :surveyIds " +
           "GROUP BY q.surveyId")
    List<Object[]> countDistinctResponseSessionsBySurveyIds(@Param("surveyIds") List<Long> surveyIds);
}
