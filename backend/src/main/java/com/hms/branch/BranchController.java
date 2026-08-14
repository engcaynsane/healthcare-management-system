package com.hms.branch;

import com.hms.common.ApiResponse;
import com.hms.common.PagedResponse;
import com.hms.common.exception.DuplicateResourceException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.audit.AuditService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchRepository repository;
    private final AuditService auditService;

    public record BranchRequest(@NotBlank String name, @NotBlank String code, String address,
                                String phone, String email, Boolean active, Boolean central) {
    }

    public record BranchResponse(Long id, String name, String code, String address, String phone,
                                 String email, boolean active, boolean central) {
        static BranchResponse from(Branch b) {
            return new BranchResponse(b.getId(), b.getName(), b.getCode(), b.getAddress(),
                    b.getPhone(), b.getEmail(), b.isActive(), b.isCentral());
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('branch.view','user.view')")
    public ApiResponse<PagedResponse<BranchResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var paged = repository.search(q, PageRequest.of(page, size, Sort.by("name")));
        return ApiResponse.ok(PagedResponse.of(paged.map(BranchResponse::from)));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyAuthority('branch.view','user.view','sale.view')")
    public ApiResponse<List<BranchResponse>> all() {
        return ApiResponse.ok(repository.findAll(Sort.by("name")).stream().map(BranchResponse::from).toList());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('branch.create')")
    public ApiResponse<BranchResponse> create(@Valid @RequestBody BranchRequest req) {
        if (repository.existsByCode(req.code().toUpperCase())) {
            throw new DuplicateResourceException("Branch code already exists");
        }
        Branch branch = Branch.builder()
                .name(req.name())
                .code(req.code().toUpperCase())
                .address(req.address())
                .phone(req.phone())
                .email(req.email())
                .active(req.active() == null || req.active())
                .central(Boolean.TRUE.equals(req.central()))
                .build();
        Branch saved = repository.save(branch);
        auditService.log("BRANCH_CREATE", "Created branch " + saved.getName());
        return ApiResponse.ok(BranchResponse.from(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('branch.update')")
    public ApiResponse<BranchResponse> update(@PathVariable Long id, @Valid @RequestBody BranchRequest req) {
        Branch branch = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        branch.setName(req.name());
        branch.setAddress(req.address());
        branch.setPhone(req.phone());
        branch.setEmail(req.email());
        if (req.active() != null) {
            branch.setActive(req.active());
        }
        if (req.central() != null) {
            branch.setCentral(req.central());
        }
        Branch saved = repository.save(branch);
        auditService.log("BRANCH_UPDATE", "Updated branch " + saved.getName());
        return ApiResponse.ok(BranchResponse.from(saved));
    }
}