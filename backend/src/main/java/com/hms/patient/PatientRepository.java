package com.hms.patient;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByPatientCode(String patientCode);

    boolean existsByPatientCode(String patientCode);

    @Query("select count(p) from Patient p where p.branchId = :branchId and p.active = true and cast(p.createdAt as date) = :date")
    long countByBranchAndDate(@Param("branchId") Long branchId, @Param("date") LocalDate date);

    @Query("select p from Patient p where p.active = true and (:branchId is null or p.branchId = :branchId) and " +
            "(:search is null or lower(p.firstName) like lower(concat('%', :search, '%')) " +
            "or lower(p.lastName) like lower(concat('%', :search, '%')) " +
            "or lower(p.phone) like lower(concat('%', :search, '%')) " +
            "or lower(p.patientCode) like lower(concat('%', :search, '%')))")
    Page<Patient> search(@Param("branchId") Long branchId,
                         @Param("search") String search,
                         Pageable pageable);

    @Query("select p from Patient p " +
            "where p.active = true and (:branchId is null or p.branchId = :branchId) " +
            "and exists (select 1 from Appointment a where a.patient.id = p.id and a.doctor.id = :doctorId) " +
            "and (:search is null or lower(p.firstName) like lower(concat('%', :search, '%')) " +
            "or lower(p.lastName) like lower(concat('%', :search, '%')) " +
            "or lower(p.phone) like lower(concat('%', :search, '%')) " +
            "or lower(p.patientCode) like lower(concat('%', :search, '%')))")
    Page<Patient> searchForDoctor(@Param("branchId") Long branchId,
                                  @Param("doctorId") Long doctorId,
                                  @Param("search") String search,
                                  Pageable pageable);

    @Query("select case when count(a) > 0 then true else false end from Appointment a " +
            "where a.patient.id = :patientId and a.doctor.id = :doctorId")
    boolean existsForDoctor(@Param("patientId") Long patientId, @Param("doctorId") Long doctorId);
}