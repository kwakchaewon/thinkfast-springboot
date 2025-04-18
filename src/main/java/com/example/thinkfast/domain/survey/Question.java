package com.example.thinkfast.domain.survey;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "QUESTIONS")
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "SURVEY_ID", nullable = false)
    private Long surveyId;

    @Enumerated(EnumType.STRING)
    private QuestionType type;

    private String content;

    private int orderIndex;

    @Getter
    public enum QuestionType {
        MULTIPLE_CHOICE, SUBJECTIVE, SCALE
    }
}
