package com.hms.doctor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByUserId(Long userId);

    @Query("select d from Doctor d where (:branchId is null or d.branchId = :branchId) and d.active = true " +
            "and (:search is null or lower(d.firstName) like lower(concat('%', :search, '%')) " +
            "or lower(d.lastName) like lower(concat('%', :search, '%')) " +
            "or lower(d.specialty) like lower(concat('%', :search, '%')))")
    List<Doctor> search(@Param("branchId") Long branchId, @Param("search") String search);
}