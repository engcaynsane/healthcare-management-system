package com.hms.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("select a from AuditLog a where (:branchId is null or a.branchId = :branchId) " +
            "and (:action is null or lower(a.action) like lower(concat('%', :action, '%'))) " +
            "and (:username is null or lower(a.username) = lower(:username))")
    Page<AuditLog> search(@Param("branchId") Long branchId,
                          @Param("action") String action,
                          @Param("username") String username,
                          Pageable pageable);
}