package com.hms.supplier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    boolean existsByName(String name);

    @Query("select s from Supplier s where :search is null or lower(s.name) like lower(concat('%', :search, '%')) " +
            "or lower(s.contactPerson) like lower(concat('%', :search, '%'))")
    Page<Supplier> search(@Param("search") String search, Pageable pageable);
}