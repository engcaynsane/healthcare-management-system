package com.hms.appointment;

import com.hms.common.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("select a from Appointment a where (:branchId is null or a.branchId = :branchId) " +
            "and (:date is null or cast(a.startTime as date) = :date) " +
            "and (:status is null or a.status = :status) " +
            "and (:doctorId is null or a.doctor.id = :doctorId)")
    Page<Appointment> search(@Param("branchId") Long branchId,
                             @Param("date") LocalDate date,
                             @Param("status") AppointmentStatus status,
                             @Param("doctorId") Long doctorId,
                             Pageable pageable);

    @Query("select count(a) from Appointment a where a.branchId = :branchId " +
            "and cast(a.startTime as date) = :date and a.status <> com.hms.common.enums.AppointmentStatus.CANCELLED")
    long countToday(@Param("branchId") Long branchId, @Param("date") LocalDate date);
}