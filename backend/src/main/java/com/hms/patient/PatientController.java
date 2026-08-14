package com.hms.patient;

import com.hms.audit.AuditService;
import com.hms.common.ApiResponse;
import com.hms.common.BranchContext;
import com.hms.common.PagedResponse;
import com.hms.common.exception.DuplicateResourceException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.doctor.DoctorScope;
import com.hms.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientRepository repository;
    private final AuditService auditService;
    private final DoctorScope doctorScope;

    public record PatientRequest(@NotBlank String firstName, @NotBlank String lastName, String gender,
                                 LocalDate dateOfBirth, String phone, String email, String bloodGroup,
                                 String nationalId, String allergies, String medicalHistory,
                                 String emergencyContactName, String emergencyContactPhone) {
    }

    public record PatientResponse(Long id, String patientCode, String firstName, String lastName,
                                  String gender, LocalDate dateOfBirth, String phone, String email,
                                  String bloodGroup, String nationalId, String allergies,
                                  String medicalHistory, String emergencyContactName,
                                  String emergencyContactPhone, boolean active, Long branchId) {
        static PatientResponse from(Patient p) {
            return new PatientResponse(p.getId(), p.getPatientCode(), p.getFirstName(), p.getLastName(),
                    p.getGender(), p.getDateOfBirth(), p.getPhone(), p.getEmail(), p.getBloodGroup(),
                    p.getNationalId(), p.getAllergies(), p.getMedicalHistory(),
                    p.getEmergencyContactName(), p.getEmergencyContactPhone(), p.isActive(), p.getBranchId());
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('patient.view')")
    public ApiResponse<PagedResponse<PatientResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Long doctorId = doctorScope.currentDoctorId().orElse(null);
        var paged = doctorId != null
                ? repository.searchForDoctor(resolveBranch(branchId), doctorId, q, pageable)
                : repository.search(resolveBranch(branchId), q, pageable);
        return ApiResponse.ok(PagedResponse.of(paged.map(PatientResponse::from)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('patient.view')")
    public ApiResponse<PatientResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(PatientResponse.from(find(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('patient.create')")
    public ApiResponse<PatientResponse> create(@Valid @RequestBody PatientRequest req) {
        String code = generateCode();
        while (repository.existsByPatientCode(code)) {
            code = generateCode();
        }
        Patient patient = Patient.builder()
                .branchId(BranchContext.branchId())
                .patientCode(code)
                .firstName(req.firstName())
                .lastName(req.lastName())
                .gender(req.gender())
                .dateOfBirth(req.dateOfBirth())
                .phone(req.phone())
                .email(req.email())
                .bloodGroup(req.bloodGroup())
                .nationalId(req.nationalId())
                .allergies(req.allergies())
                .medicalHistory(req.medicalHistory())
                .emergencyContactName(req.emergencyContactName())
                .emergencyContactPhone(req.emergencyContactPhone())
                .active(true)
                .build();
        Patient saved = repository.save(patient);
        auditService.log("PATIENT_CREATE", "Registered patient " + code + " " + saved.getFirstName());
        return ApiResponse.ok(PatientResponse.from(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('patient.update')")
    public ApiResponse<PatientResponse> update(@PathVariable Long id, @Valid @RequestBody PatientRequest req) {
        Patient patient = find(id);
        patient.setFirstName(req.firstName());
        patient.setLastName(req.lastName());
        patient.setGender(req.gender());
        patient.setDateOfBirth(req.dateOfBirth());
        patient.setPhone(req.phone());
        patient.setEmail(req.email());
        patient.setBloodGroup(req.bloodGroup());
        patient.setNationalId(req.nationalId());
        patient.setAllergies(req.allergies());
        patient.setMedicalHistory(req.medicalHistory());
        patient.setEmergencyContactName(req.emergencyContactName());
        patient.setEmergencyContactPhone(req.emergencyContactPhone());
        return ApiResponse.ok(PatientResponse.from(repository.save(patient)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('patient.delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Patient patient = find(id);
        patient.setActive(false);
        repository.save(patient);
        auditService.log("PATIENT_DELETE", "Deleted patient " + patient.getPatientCode() + " " + patient.getFirstName());
        return ApiResponse.ok("Patient deleted", null);
    }

    private Patient find(Long id) {
        Patient patient = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
        SecurityUtils.requireSameBranch(patient.getBranchId(), "Patient not found");
        Long doctorId = doctorScope.currentDoctorId().orElse(null);
        if (doctorId != null && !repository.existsForDoctor(id, doctorId)) {
            throw new ResourceNotFoundException("Patient not found");
        }
        return patient;
    }

    private Long resolveBranch(Long branchId) {
        Long current = BranchContext.branchId();
        if (current != null) {
            return current;
        }
        if (SecurityUtils.isSuperAdmin()) {
            return branchId;
        }
        return Long.MIN_VALUE;
    }

    private String generateCode() {
        return "PT-" + System.currentTimeMillis() % 1_000_000;
    }
}