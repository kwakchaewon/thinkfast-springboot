package com.example.thinkfast.domain.ai;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 인사이트 리포트 엔티티
 * 설문 종료 후 생성된 요약 리포트를 저장
 */
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "INSIGHT_REPORTS")
public class InsightReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "SURVEY_ID", nullable = false, unique = true)
    private Long surveyId;

    @Column(name = "SUMMARY_TEXT", columnDefinition = "TEXT")
    private String summaryText; // JSON으로 직렬화된 SummaryReportDto

    @Column(name = "KEYWORDS", columnDefinition = "TEXT")
    private String keywords; // JSON 형식의 키워드 리스트

    @Column(name = "SENTIMENT_SUMMARY", columnDefinition = "TEXT")
    private String sentimentSummary; // 향후 사용 가능

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}

