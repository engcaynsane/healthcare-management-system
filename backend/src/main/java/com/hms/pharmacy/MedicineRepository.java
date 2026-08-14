package com.hms.pharmacy;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {

    Optional<Medicine> findByBarcode(String barcode);

    boolean existsByBarcode(String barcode);

    @Query("select m from Medicine m where (:search is null or lower(m.name) like lower(concat('%', :search, '%')) " +
            "or lower(m.genericName) like lower(concat('%', :search, '%')) " +
            "or lower(m.barcode) like lower(concat('%', :search, '%')) " +
            "or lower(m.brand) like lower(concat('%', :search, '%'))) " +
            "and (:categoryId is null or m.category.id = :categoryId)")
    Page<Medicine> search(@Param("search") String search,
                          @Param("categoryId") Long categoryId,
                          Pageable pageable);
}