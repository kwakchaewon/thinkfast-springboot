package com.example.thinkfast.domain.survey;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 하는 이유?
@AllArgsConstructor
@Table(name = "RESPONSES")
public class Response {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "RESPONSE_SESSION_ID", nullable = false)
    private String responseSessionId; // 추후 응답자 전략 수립 이후 지정 예정
    @Column(name = "QUESTION_ID", nullable = false)
    private Long questionId;
    @Column(name = "QUESTION_TYPE", nullable = false)
    private String questionType;
    @Column(name = "OPTION_ID")
    private Long optionId;
    @Column(name = "SUBJECTIVE_CONTENT")
    private String subjectiveContent;
    @Column(name = "SCALE_VALUE")
    private Integer scaleValue;
}