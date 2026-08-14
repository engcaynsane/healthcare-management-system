package com.hms.inventory;

import com.hms.common.enums.TransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InventoryTransferRepository extends JpaRepository<InventoryTransfer, Long> {

    @Query("select t from InventoryTransfer t where (:branchId is null or t.branchId = :branchId " +
            "or t.toBranchId = :branchId) and (:status is null or t.status = :status)")
    Page<InventoryTransfer> search(@Param("branchId") Long branchId,
                                   @Param("status") TransferStatus status,
                                   Pageable pageable);

    List<InventoryTransfer> findByToBranchIdAndStatus(Long toBranchId, TransferStatus status);
}