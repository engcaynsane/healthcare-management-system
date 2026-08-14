package com.hms.sale;

import com.hms.common.ApiResponse;
import com.hms.common.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    @PostMapping
    @PreAuthorize("hasAuthority('sale.create')")
    public ApiResponse<SaleService.SaleDetail> create(@RequestBody SaleService.SaleRequest req) {
        return ApiResponse.ok(saleService.detail(saleService.create(req).getId()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('sale.view')")
    public ApiResponse<PagedResponse<SaleService.SaleSummary>> list(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(PagedResponse.of(saleService.search(date, status, q, page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sale.view')")
    public ApiResponse<SaleService.SaleDetail> get(@PathVariable Long id) {
        return ApiResponse.ok(saleService.detail(id));
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasAuthority('sale.refund')")
    public ApiResponse<SaleService.SaleDetail> refund(@PathVariable Long id,
                                                      @RequestBody(required = false) RefundRequest req) {
        String reason = req != null ? req.reason() : null;
        return ApiResponse.ok(saleService.detail(saleService.refund(id, reason).getId()));
    }

    public record RefundRequest(String reason) {
    }
}