package com.hms.doctor;

import com.hms.common.BranchContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Resolves whether the currently authenticated user is a linked doctor
 * that the application should scope data to.
 */
@Component
@RequiredArgsConstructor
public class DoctorScope {

    private final DoctorRepository doctorRepository;

    /** The Doctor record id for the current user, if they are a linked, active doctor. */
    public Optional<Long> currentDoctorId() {
        if (BranchContext.userId() == null) {
            return Optional.empty();
        }
        return doctorRepository.findByUserId(BranchContext.userId())
                .filter(Doctor::isActive)
                .map(Doctor::getId);
    }

    public boolean isDoctor() {
        return currentDoctorId().isPresent();
    }
}