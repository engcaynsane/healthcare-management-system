package com.hms.supplier;

import com.hms.common.ApiResponse;
import com.hms.common.PagedResponse;
import com.hms.common.exception.DuplicateResourceException;
import com.hms.common.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierRepository repository;

    public record SupplierRequest(@NotBlank String name, String contactPerson, String phone,
                                  String email, String address) {
    }

    public record SupplierResponse(Long id, String name, String contactPerson, String phone,
                                   String email, String address, boolean active) {
        static SupplierResponse from(Supplier s) {
            return new SupplierResponse(s.getId(), s.getName(), s.getContactPerson(), s.getPhone(),
                    s.getEmail(), s.getAddress(), s.isActive());
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('supplier.view')")
    public ApiResponse<PagedResponse<SupplierResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var paged = repository.search(q, PageRequest.of(page, size, Sort.by("name")));
        return ApiResponse.ok(PagedResponse.of(paged.map(SupplierResponse::from)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('supplier.create')")
    public ApiResponse<SupplierResponse> create(@Valid @RequestBody SupplierRequest req) {
        if (repository.existsByName(req.name())) {
            throw new DuplicateResourceException("Supplier already exists");
        }
        return ApiResponse.ok(SupplierResponse.from(repository.save(Supplier.builder()
                .name(req.name())
                .contactPerson(req.contactPerson())
                .phone(req.phone())
                .email(req.email())
                .address(req.address())
                .active(true)
                .build())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('supplier.update')")
    public ApiResponse<SupplierResponse> update(@PathVariable Long id, @Valid @RequestBody SupplierRequest req) {
        Supplier supplier = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
        supplier.setName(req.name());
        supplier.setContactPerson(req.contactPerson());
        supplier.setPhone(req.phone());
        supplier.setEmail(req.email());
        supplier.setAddress(req.address());
        return ApiResponse.ok(SupplierResponse.from(repository.save(supplier)));
    }
}