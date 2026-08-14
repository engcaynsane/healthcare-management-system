package com.hms.lab;

import com.hms.common.enums.LabOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LabOrderRepository extends JpaRepository<LabOrder, Long> {

    @Query("select o from LabOrder o where (:branchId is null or o.branchId = :branchId) " +
            "and (:status is null or o.status = :status)")
    Page<LabOrder> search(@Param("branchId") Long branchId,
                          @Param("status") LabOrderStatus status,
                          Pageable pageable);

    @Query("select o from LabOrder o where (:branchId is null or o.branchId = :branchId) " +
            "and (:status is null or o.status = :status) and o.requestedByUserId = :requestedByUserId")
    Page<LabOrder> searchRequestedBy(@Param("branchId") Long branchId,
                                     @Param("requestedByUserId") Long requestedByUserId,
                                     @Param("status") LabOrderStatus status,
                                     Pageable pageable);

    boolean existsByIdAndRequestedByUserId(Long id, Long requestedByUserId);

    @Query("select count(o) from LabOrder o where o.branchId = :branchId and o.status in " +
            "(com.hms.common.enums.LabOrderStatus.REQUESTED, " +
            "com.hms.common.enums.LabOrderStatus.SAMPLE_COLLECTED, " +
            "com.hms.common.enums.LabOrderStatus.IN_PROGRESS)")
    long countPending(@Param("branchId") Long branchId);
}