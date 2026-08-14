package com.hms.appointment;

import com.hms.common.ApiResponse;
import com.hms.common.PagedResponse;
import com.hms.common.enums.AppointmentStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService service;

    @GetMapping
    @PreAuthorize("hasAuthority('appointment.view')")
    public ApiResponse<PagedResponse<AppointmentService.AppointmentSummary>> list(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AppointmentStatus apptStatus = status != null ? AppointmentStatus.valueOf(status) : null;
        return ApiResponse.ok(PagedResponse.of(service.search(date, apptStatus, doctorId, page, size)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('appointment.create')")
    public ApiResponse<AppointmentService.AppointmentSummary> create(
            @Valid @RequestBody AppointmentService.AppointmentRequest req) {
        return ApiResponse.ok(service.create(req));
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('appointment.update')")
    public ApiResponse<AppointmentService.AppointmentSummary> updateStatus(@PathVariable Long id,
                                                                           @RequestBody StatusRequest req) {
        return ApiResponse.ok(service.updateStatus(id, req.status()));
    }

    public record StatusRequest(String status) {
    }
}