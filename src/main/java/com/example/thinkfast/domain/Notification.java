package com.example.thinkfast.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "NOTIFICATIONS")
@Setter
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String type; // 알람 유형 (예: SURVEY_RESPONSE)
    private Long recipientId; // 수신자 (회원 ID)
    private String message; // 알람 내용
    private Long referenceId; // 연관된 설문 ID 등
    private Boolean isRead; // 읽음 여부
    private LocalDateTime createdAt; // 생성일
    private LocalDateTime expiresAt; // 만료일

    @PrePersist
    protected void onCreate() {
        if (isRead == null){
            isRead = false;
        }

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (expiresAt == null) {
            expiresAt = createdAt.plusDays(7);
        }
    }
}
