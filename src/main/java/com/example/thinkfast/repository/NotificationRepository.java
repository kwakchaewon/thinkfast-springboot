package com.example.thinkfast.repository;

import com.example.thinkfast.domain.Notification;
import com.example.thinkfast.realtime.ResponseCreatedAlarm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 개선 사항: Redis 알람 집계 처리 및 클라이언트에게 빠르고 효율적으로 알림 전달
     * @param recipientId
     * @return
     */
    @Query("SELECT new com.example.thinkfast.realtime.ResponseCreatedAlarm(" +
            "n.type, s.id, s.title, n.isRead, MAX(n.createdAt), COUNT(n)) " +
            "FROM Notification n " +
            "JOIN Survey s ON s.id = n.referenceId " +
            "WHERE n.recipientId = :recipientId AND s.isDeleted = false " +
            "GROUP BY n.referenceId " +
            "ORDER BY MAX(n.createdAt) DESC")
    List<ResponseCreatedAlarm> findNotificationSummariesByRecipient(@Param("recipientId") Long recipientId);
}
