package com.hms.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Query("select c from Customer c where (:branchId is null or c.branchId = :branchId) and " +
            "(:search is null or lower(c.name) like lower(concat('%', :search, '%')) " +
            "or lower(c.phone) like lower(concat('%', :search, '%')) " +
            "or lower(c.email) like lower(concat('%', :search, '%')))")
    Page<Customer> search(@Param("branchId") Long branchId,
                          @Param("search") String search,
                          Pageable pageable);
}