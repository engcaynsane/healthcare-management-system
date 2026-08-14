package com.hms.billing;

import com.hms.common.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @Query("select i from Invoice i where (:branchId is null or i.branchId = :branchId) and " +
            "(:date is null or cast(i.createdAt as date) = :date) and " +
            "(:status is null or i.status = :status)")
    Page<Invoice> search(@Param("branchId") Long branchId,
                         @Param("date") LocalDate date,
                         @Param("status") PaymentStatus status,
                         Pageable pageable);
}