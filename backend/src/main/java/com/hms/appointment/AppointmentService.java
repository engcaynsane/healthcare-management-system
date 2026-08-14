package com.hms.appointment;

import com.hms.audit.AuditService;
import com.hms.common.BranchContext;
import com.hms.common.enums.AppointmentStatus;
import com.hms.common.exception.BadRequestException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.doctor.Doctor;
import com.hms.doctor.DoctorRepository;
import com.hms.doctor.DoctorScope;
import com.hms.notification.NotificationService;
import com.hms.patient.Patient;
import com.hms.patient.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository repository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorScope doctorScope;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public Page<AppointmentSummary> search(LocalDate date, AppointmentStatus status, Long doctorId,
                                           int page, int size) {
        Long scopedDoctorId = doctorScope.isDoctor() ? doctorScope.currentDoctorId().get() : doctorId;
        return repository.search(BranchContext.branchId(), date, status, scopedDoctorId,
                        PageRequest.of(page, size, Sort.by("startTime")))
                .map(this::summary);
    }

    @Transactional
    public AppointmentSummary create(AppointmentRequest req) {
        Patient patient = patientRepository.findById(req.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
        Long doctorId = doctorScope.isDoctor() ? doctorScope.currentDoctorId().get() : req.doctorId();
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
        if (req.startTime() == null) {
            throw new BadRequestException("Start time is required");
        }
        Appointment appointment = Appointment.builder()
                .branchId(BranchContext.branchId())
                .patient(patient)
                .doctor(doctor)
                .startTime(req.startTime())
                .endTime(req.endTime())
                .status(AppointmentStatus.SCHEDULED)
                .purpose(req.purpose())
                .notes(req.notes())
                .build();
        Appointment saved = repository.save(appointment);
        auditService.log("APPOINTMENT_CREATE", "Appointment for " + patient.getFirstName() +
                " scheduled for " + req.startTime());
        notifyAppointment(patient, doctor, saved);
        return summary(saved);
    }

    private void notifyAppointment(Patient patient, Doctor doctor, Appointment appointment) {
        String title = "New appointment";
        String message = patient.getFirstName() + " " + patient.getLastName() +
                " scheduled " + appointment.getStartTime() + " with Dr. " +
                doctor.getFirstName() + " " + doctor.getLastName();
        notificationService.notifyBranch(appointment.getBranchId(), title, message, "APPOINTMENT");
    }

    @Transactional
    public AppointmentSummary updateStatus(Long id, String status) {
        Appointment appointment = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        if (doctorScope.isDoctor() && !appointment.getDoctor().getId().equals(doctorScope.currentDoctorId().get())) {
            throw new ResourceNotFoundException("Appointment not found");
        }
        AppointmentStatus newStatus;
        try {
            newStatus = AppointmentStatus.valueOf(status);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid appointment status");
        }
        appointment.setStatus(newStatus);
        auditService.log("APPOINTMENT_STATUS", "Appointment " + id + " -> " + newStatus);
        return summary(repository.save(appointment));
    }

    private AppointmentSummary summary(Appointment a) {
        return new AppointmentSummary(a.getId(),
                a.getPatient().getId(), a.getPatient().getFirstName() + " " + a.getPatient().getLastName(),
                a.getDoctor().getId(), a.getDoctor().getFirstName() + " " + a.getDoctor().getLastName(),
                a.getStartTime(), a.getEndTime(),
                a.getStatus() != null ? a.getStatus().name() : null,
                a.getPurpose(), a.getNotes());
    }

    public record AppointmentRequest(Long patientId, Long doctorId, LocalDateTime startTime,
                                     LocalDateTime endTime, String purpose, String notes) {
    }

    public record AppointmentSummary(Long id, Long patientId, String patientName, Long doctorId,
                                     String doctorName, LocalDateTime startTime, LocalDateTime endTime,
                                     String status, String purpose, String notes) {
    }
}