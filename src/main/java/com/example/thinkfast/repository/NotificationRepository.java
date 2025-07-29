package com.example.thinkfast.repository;

import com.example.thinkfast.domain.Notification;
import com.example.thinkfast.realtime.dto.ResponseCreatedAlarm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 개선 사항: Redis 알람 집계 처리 및 클라이언트에게 빠르고 효율적으로 알림 전달
     * @param recipientId
     * @return
     */
    @Query("SELECT new com.example.thinkfast.realtime.ResponseCreatedAlarm(" +
            "n.type, s.id, s.title, n.isRead, n.createdAt, " +
            "(SELECT COUNT(n2) FROM Notification n2 WHERE n2.referenceId = n.referenceId)) " +
            "FROM Notification n " +
            "JOIN Survey s ON s.id = n.referenceId " +
            "WHERE n.recipientId = :recipientId " +
            "AND s.isDeleted = false " +
            "AND n.createdAt = (" +
            "    SELECT MAX(n3.createdAt) FROM Notification n3 " +
            "    WHERE n3.referenceId = n.referenceId AND n3.createdAt > :monthAgo" +
            ") " +
            "ORDER BY n.createdAt DESC")
    List<ResponseCreatedAlarm> findNotificationSummariesByRecipient(@Param("recipientId") Long recipientId, @Param("monthAgo") LocalDateTime monthAgo);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true " +
            "WHERE n.type = 'SURVEY_RESPONSE' " +
            "AND n.recipientId = :recipientId " +
            "AND n.referenceId IN :surveyIds")
    int updateSurveyNotificationAsRead(@Param("recipientId") Long recipientId,
                                  @Param("surveyIds") List<Long> surveyIds);
}
