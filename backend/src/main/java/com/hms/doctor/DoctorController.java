package com.hms.doctor;

import com.hms.audit.AuditService;
import com.hms.auth.domain.User;
import com.hms.auth.repository.UserRepository;
import com.hms.common.ApiResponse;
import com.hms.common.BranchContext;
import com.hms.common.exception.BadRequestException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorRepository repository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public record DoctorRequest(Long userId, @NotBlank String firstName, @NotBlank String lastName,
                                String specialty, String licenseNumber, BigDecimal consultationFee) {
    }

    public record DoctorResponse(Long id, Long userId, String userName, String fullName,
                                 String specialty, String licenseNumber, BigDecimal consultationFee,
                                 boolean active) {
        static DoctorResponse from(Doctor d) {
            String uname = d.getUser() != null ? d.getUser().getUsername() : null;
            String full = d.getFirstName() + " " + d.getLastName();
            return new DoctorResponse(d.getId(), d.getUser() != null ? d.getUser().getId() : null,
                    uname, full, d.getSpecialty(), d.getLicenseNumber(), d.getConsultationFee(), d.isActive());
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('doctor.view','appointment.create','appointment.view')")
    public ApiResponse<List<DoctorResponse>> list(@RequestParam(required = false) String q) {
        return ApiResponse.ok(repository.search(BranchContext.branchId(), q).stream()
                .map(DoctorResponse::from).toList());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('doctor.assign')")
    public ApiResponse<DoctorResponse> create(@Valid @RequestBody DoctorRequest req) {
        User linkedUser = null;
        if (req.userId() != null) {
            linkedUser = userRepository.findById(req.userId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            SecurityUtils.requireSameBranch(linkedUser.getBranchId(), "User not found");
        }
        Doctor doctor = Doctor.builder()
                .branchId(BranchContext.branchId())
                .user(linkedUser)
                .firstName(req.firstName())
                .lastName(req.lastName())
                .specialty(req.specialty())
                .licenseNumber(req.licenseNumber())
                .consultationFee(req.consultationFee())
                .active(true)
                .build();
        Doctor saved = repository.save(doctor);
        auditService.log("DOCTOR_CREATE", "Created doctor " + saved.getFirstName());
        return ApiResponse.ok(DoctorResponse.from(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('doctor.assign')")
    public ApiResponse<DoctorResponse> update(@PathVariable Long id, @Valid @RequestBody DoctorRequest req) {
        Doctor doctor = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
        SecurityUtils.requireSameBranch(doctor.getBranchId(), "Doctor not found");
        doctor.setFirstName(req.firstName());
        doctor.setLastName(req.lastName());
        doctor.setSpecialty(req.specialty());
        doctor.setLicenseNumber(req.licenseNumber());
        doctor.setConsultationFee(req.consultationFee());
        if (req.userId() != null) {
            User linkedUser = userRepository.findById(req.userId())
                    .orElseThrow(() -> new BadRequestException("User not found"));
            SecurityUtils.requireSameBranch(linkedUser.getBranchId(), "User not found");
            doctor.setUser(linkedUser);
        }
        return ApiResponse.ok(DoctorResponse.from(repository.save(doctor)));
    }
}