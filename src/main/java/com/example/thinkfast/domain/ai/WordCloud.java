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
 * 워드클라우드 엔티티
 * 설문 종료 후 생성된 워드클라우드 데이터를 저장
 */
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "WORD_CLOUDS")
public class WordCloud {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "QUESTION_ID", nullable = false, unique = true)
    private Long questionId;

    @Column(name = "WORD_CLOUD_DATA", columnDefinition = "TEXT")
    private String wordCloudData; // JSON으로 직렬화된 WordCloudResponseDto

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}

