package com.hms.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MedicineBatchRepository extends JpaRepository<MedicineBatch, Long> {

    Optional<MedicineBatch> findByMedicineIdAndBatchNoAndBranchId(Long medicineId, String batchNo, Long branchId);

    @Query("select b from MedicineBatch b where b.medicine.id = :medicineId and b.branchId = :branchId " +
            "order by b.expiryDate asc")
    List<MedicineBatch> findForFefo(@Param("medicineId") Long medicineId, @Param("branchId") Long branchId);
}