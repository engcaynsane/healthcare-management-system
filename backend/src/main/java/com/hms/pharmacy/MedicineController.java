package com.hms.pharmacy;

import com.hms.audit.AuditService;
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

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineRepository medicineRepository;
    private final MedicineCategoryRepository categoryRepository;
    private final AuditService auditService;

    public record CategoryRequest(@NotBlank String name, String description) {
    }

    public record CategoryResponse(Long id, String name, String description) {
        static CategoryResponse from(MedicineCategory c) {
            return new CategoryResponse(c.getId(), c.getName(), c.getDescription());
        }
    }

    public record MedicineRequest(@NotBlank String name, String genericName, String brand, Long categoryId,
                                  String strength, String dosageForm, @NotBlank String barcode,
                                  String packSize, String unit, Integer reorderLevel,
                                  Boolean requirePrescription, BigDecimal sellingPrice, BigDecimal costPrice) {
    }

    public record MedicineResponse(Long id, String name, String genericName, String brand,
                                   Long categoryId, String categoryName, String strength, String dosageForm,
                                   String barcode, String packSize, String unit, int reorderLevel,
                                   boolean requirePrescription, BigDecimal sellingPrice, BigDecimal costPrice,
                                   boolean active) {
        static MedicineResponse from(Medicine m) {
            return new MedicineResponse(m.getId(), m.getName(), m.getGenericName(), m.getBrand(),
                    m.getCategory() != null ? m.getCategory().getId() : null,
                    m.getCategory() != null ? m.getCategory().getName() : null,
                    m.getStrength(), m.getDosageForm(), m.getBarcode(), m.getPackSize(), m.getUnit(),
                    m.getReorderLevel(), m.isRequirePrescription(), m.getSellingPrice(), m.getCostPrice(),
                    m.isActive());
        }
    }

    // ---- Categories ----

    @GetMapping("/api/medicine-categories")
    @PreAuthorize("hasAuthority('medicine.view')")
    public ApiResponse<List<CategoryResponse>> categories() {
        return ApiResponse.ok(categoryRepository.findAll(Sort.by("name")).stream()
                .map(CategoryResponse::from).toList());
    }

    @PostMapping("/api/medicine-categories")
    @PreAuthorize("hasAuthority('medicine.create')")
    public ApiResponse<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest req) {
        if (categoryRepository.existsByName(req.name())) {
            throw new DuplicateResourceException("Category already exists");
        }
        MedicineCategory saved = categoryRepository.save(MedicineCategory.builder()
                .name(req.name()).description(req.description()).build());
        return ApiResponse.ok(CategoryResponse.from(saved));
    }

    // ---- Medicines ----

    @GetMapping("/api/medicines")
    @PreAuthorize("hasAuthority('medicine.view')")
    public ApiResponse<PagedResponse<MedicineResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var paged = medicineRepository.search(q, categoryId, PageRequest.of(page, size, Sort.by("name")));
        return ApiResponse.ok(PagedResponse.of(paged.map(MedicineResponse::from)));
    }

    @GetMapping("/api/medicines/{id}")
    @PreAuthorize("hasAuthority('medicine.view')")
    public ApiResponse<MedicineResponse> get(@PathVariable Long id) {
        Medicine m = medicineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found"));
        return ApiResponse.ok(MedicineResponse.from(m));
    }

    @PostMapping("/api/medicines")
    @PreAuthorize("hasAuthority('medicine.create')")
    public ApiResponse<MedicineResponse> create(@Valid @RequestBody MedicineRequest req) {
        if (medicineRepository.existsByBarcode(req.barcode())) {
            throw new DuplicateResourceException("Barcode already exists");
        }
        Medicine medicine = Medicine.builder()
                .name(req.name())
                .genericName(req.genericName())
                .brand(req.brand())
                .category(resolveCategory(req.categoryId()))
                .strength(req.strength())
                .dosageForm(req.dosageForm())
                .barcode(req.barcode())
                .packSize(req.packSize())
                .unit(req.unit())
                .reorderLevel(req.reorderLevel() == null ? 0 : req.reorderLevel())
                .requirePrescription(req.requirePrescription() == null || req.requirePrescription())
                .sellingPrice(req.sellingPrice())
                .costPrice(req.costPrice())
                .active(true)
                .build();
        Medicine saved = medicineRepository.save(medicine);
        auditService.log("MEDICINE_CREATE", "Created medicine " + saved.getName());
        return ApiResponse.ok(MedicineResponse.from(saved));
    }

    @PutMapping("/api/medicines/{id}")
    @PreAuthorize("hasAuthority('medicine.update')")
    public ApiResponse<MedicineResponse> update(@PathVariable Long id, @Valid @RequestBody MedicineRequest req) {
        Medicine m = medicineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found"));
        m.setName(req.name());
        m.setGenericName(req.genericName());
        m.setBrand(req.brand());
        m.setCategory(resolveCategory(req.categoryId()));
        m.setStrength(req.strength());
        m.setDosageForm(req.dosageForm());
        m.setPackSize(req.packSize());
        m.setUnit(req.unit());
        m.setReorderLevel(req.reorderLevel() == null ? 0 : req.reorderLevel());
        m.setRequirePrescription(req.requirePrescription() == null || req.requirePrescription());
        m.setSellingPrice(req.sellingPrice());
        m.setCostPrice(req.costPrice());
        return ApiResponse.ok(MedicineResponse.from(medicineRepository.save(m)));
    }

    private MedicineCategory resolveCategory(Long id) {
        if (id == null) {
            return null;
        }
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }
}