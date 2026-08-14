package com.hms.customer;

import com.hms.audit.AuditService;
import com.hms.common.ApiResponse;
import com.hms.common.BranchContext;
import com.hms.common.PagedResponse;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerRepository repository;
    private final AuditService auditService;

    public record CustomerRequest(@NotBlank String name, String phone, String email, String address,
                                  BigDecimal creditLimit, String notes) {
    }

    public record CustomerResponse(Long id, String name, String phone, String email, String address,
                                   int loyaltyPoints, BigDecimal creditLimit, BigDecimal balance,
                                   String notes, boolean active) {
        static CustomerResponse from(Customer c) {
            return new CustomerResponse(c.getId(), c.getName(), c.getPhone(), c.getEmail(), c.getAddress(),
                    c.getLoyaltyPoints(), c.getCreditLimit(), c.getBalance(), c.getNotes(), c.isActive());
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('customer.view')")
    public ApiResponse<PagedResponse<CustomerResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var paged = repository.search(BranchContext.branchId(), q, PageRequest.of(page, size, Sort.by("name")));
        return ApiResponse.ok(PagedResponse.of(paged.map(CustomerResponse::from)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('customer.view')")
    public ApiResponse<CustomerResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(CustomerResponse.from(find(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('customer.create')")
    public ApiResponse<CustomerResponse> create(@Valid @RequestBody CustomerRequest req) {
        Customer customer = Customer.builder()
                .branchId(BranchContext.branchId())
                .name(req.name())
                .phone(req.phone())
                .email(req.email())
                .address(req.address())
                .creditLimit(req.creditLimit())
                .notes(req.notes())
                .balance(BigDecimal.ZERO)
                .active(true)
                .build();
        Customer saved = repository.save(customer);
        auditService.log("CUSTOMER_CREATE", "Created customer " + saved.getName());
        return ApiResponse.ok(CustomerResponse.from(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('customer.update')")
    public ApiResponse<CustomerResponse> update(@PathVariable Long id, @Valid @RequestBody CustomerRequest req) {
        Customer customer = find(id);
        customer.setName(req.name());
        customer.setPhone(req.phone());
        customer.setEmail(req.email());
        customer.setAddress(req.address());
        customer.setCreditLimit(req.creditLimit());
        customer.setNotes(req.notes());
        return ApiResponse.ok(CustomerResponse.from(repository.save(customer)));
    }

    private Customer find(Long id) {
        Customer customer = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        SecurityUtils.requireSameBranch(customer.getBranchId(), "Customer not found");
        return customer;
    }
}