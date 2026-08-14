package com.hms.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    List<Inventory> findByBranchIdAndMedicineId(Long branchId, Long medicineId);

    @Query("select i from Inventory i where i.branchId = :branchId order by i.medicine.name, i.batch.expiryDate")
    List<Inventory> findAllByBranchId(@Param("branchId") Long branchId);

    @Query("select i from Inventory i where i.branchId = :branchId and i.batch.expiryDate <= :date and i.quantity > 0 " +
            "order by i.batch.expiryDate")
    List<Inventory> findExpiring(@Param("branchId") Long branchId, @Param("date") LocalDate date);

    @Query("select i from Inventory i where i.branchId = :branchId and i.quantity > 0 " +
            "order by i.medicine.name, i.batch.expiryDate")
    List<Inventory> findAvailable(@Param("branchId") Long branchId);
}