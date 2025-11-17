package com.example.thinkfast.domain.survey;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "SURVEY_RESPONSE_HISTORY")
public class SurveyResponseHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "SURVEY_ID", nullable = false)
    private Long surveyId;

    @Column(name = "DEVICE_ID_HASH", nullable = false, length = 255)
    private String deviceIdHash;

    @Column(name = "IP_ADDRESS_HASH", length = 255)
    private String ipAddressHash;

    @Column(name = "RESPONDED_AT")
    private LocalDateTime respondedAt;

    @PrePersist
    protected void onCreate() {
        this.respondedAt = LocalDateTime.now();
    }
}
