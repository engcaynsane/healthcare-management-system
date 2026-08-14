package com.hms.sale;

import com.hms.common.enums.SaleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    Optional<Sale> findBySaleNumber(String saleNumber);

    @Query("select count(s) from Sale s " +
            "where s.branchId = :branchId and cast(s.createdAt as date) = :date " +
            "and s.status <> 'REFUNDED'")
    long countForDate(@Param("branchId") Long branchId, @Param("date") LocalDate date);

    @Query("select coalesce(sum(s.total), 0) from Sale s " +
            "where s.branchId = :branchId and cast(s.createdAt as date) = :date " +
            "and s.status <> 'REFUNDED'")
    BigDecimal sumForDate(@Param("branchId") Long branchId, @Param("date") LocalDate date);

    @Query("select s from Sale s where (:branchId is null or s.branchId = :branchId) and " +
            "(:date is null or cast(s.createdAt as date) = :date) " +
            "and (:status is null or s.status = :status) " +
            "and (:search is null or lower(s.saleNumber) like lower(concat('%', :search, '%')) " +
            "or lower(s.cashierName) like lower(concat('%', :search, '%')))")
    Page<Sale> search(@Param("branchId") Long branchId,
                      @Param("date") LocalDate date,
                      @Param("status") SaleStatus status,
                      @Param("search") String search,
                      Pageable pageable);
}