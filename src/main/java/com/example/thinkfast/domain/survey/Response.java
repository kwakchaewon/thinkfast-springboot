package com.example.thinkfast.domain.survey;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

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
    private String responseSessionId; // 추후 응답자 전략 수립 이후 지정 예정
    private Long questionId;
    private String questionType;
    private Long optionId;
    private String subjectiveContent;
    private Integer scaleValue;
}