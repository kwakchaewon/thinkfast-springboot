package com.example.thinkfast.domain.survey;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "ANSWERS")
public class Answer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long responseId;
    private Long questionId;
    private Long optionId;

    private String subjectiveAnswer;
    private Integer scaleValue;
}
