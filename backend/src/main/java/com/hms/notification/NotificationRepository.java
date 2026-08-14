package com.hms.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("select n from Notification n where (n.userId is null or n.userId = :userId) " +
            "and (:branchId is null or n.branchId = :branchId) order by n.createdAt desc")
    Page<Notification> findForUser(@Param("userId") Long userId,
                                   @Param("branchId") Long branchId,
                                   Pageable pageable);

    @Query("select count(n) from Notification n where n.read = false " +
            "and (n.userId is null or n.userId = :userId) " +
            "and (:branchId is null or n.branchId = :branchId)")
    long countUnreadForUser(@Param("userId") Long userId, @Param("branchId") Long branchId);

    @Modifying
    @Transactional
    @Query("update Notification n set n.read = true, n.readAt = :now where n.read = false " +
            "and (n.userId is null or n.userId = :userId) " +
            "and (:branchId is null or n.branchId = :branchId)")
    int markAllRead(@Param("userId") Long userId, @Param("branchId") Long branchId, @Param("now") LocalDateTime now);
}