package com.example.thinkfast.repository.ai;

import com.example.thinkfast.domain.ai.InsightReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InsightReportRepository extends JpaRepository<InsightReport, Long> {
    /**
     * 설문 ID로 인사이트 리포트 조회
     */
    Optional<InsightReport> findBySurveyId(Long surveyId);

    /**
     * 설문 ID로 인사이트 리포트 존재 여부 확인
     */
    boolean existsBySurveyId(Long surveyId);
}

