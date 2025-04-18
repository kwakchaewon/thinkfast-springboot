package com.example.thinkfast.domain.survey;

import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "RESPONSES")
public class Response {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "SURVEY_ID", nullable = false)
    private Long surveyId;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @CreationTimestamp
    private LocalDateTime submittedAt;
}
