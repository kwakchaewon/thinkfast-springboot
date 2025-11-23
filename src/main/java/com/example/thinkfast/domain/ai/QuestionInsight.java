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
 * 질문별 인사이트 엔티티
 * 설문 종료 후 생성된 질문별 인사이트 텍스트를 저장
 */
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "QUESTION_INSIGHTS")
public class QuestionInsight {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "QUESTION_ID", nullable = false, unique = true)
    private Long questionId;

    @Column(name = "INSIGHT_TEXT", columnDefinition = "TEXT")
    private String insightText; // 인사이트 텍스트

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}

