package com.hms.branch;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    Optional<Branch> findByCode(String code);

    boolean existsByCode(String code);

    @Query("select b from Branch b where :search is null or lower(b.name) like lower(concat('%', :search, '%')) " +
            "or lower(b.code) like lower(concat('%', :search, '%'))")
    Page<Branch> search(@Param("search") String search, Pageable pageable);
}