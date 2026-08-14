package com.hms.billing;

import com.hms.common.ApiResponse;
import com.hms.common.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    @GetMapping
    @PreAuthorize("hasAuthority('billing.view')")
    public ApiResponse<PagedResponse<BillingService.InvoiceSummary>> list(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(PagedResponse.of(billingService.search(date, status, page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('billing.view')")
    public ApiResponse<BillingService.InvoiceDetail> get(@PathVariable Long id) {
        return ApiResponse.ok(billingService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('billing.create')")
    public ApiResponse<BillingService.InvoiceResponse> create(@RequestBody BillingService.InvoiceRequest req) {
        return ApiResponse.ok(billingService.createInvoice(req));
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAuthority('billing.create')")
    public ApiResponse<BillingService.PaymentResponse> pay(@PathVariable Long id,
                                                           @RequestBody BillingService.PaymentRequest req) {
        return ApiResponse.ok(billingService.recordPayment(id, req));
    }
}