package com.hms.inventory;

import com.hms.common.ApiResponse;
import com.hms.common.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService service;

    @GetMapping
    @PreAuthorize("hasAuthority('inventory.view')")
    public ApiResponse<List<InventoryService.StockRow>> stock() {
        return ApiResponse.ok(service.stock(com.hms.common.BranchContext.branchId()));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAuthority('inventory.view')")
    public ApiResponse<List<InventoryService.StockRow>> lowStock() {
        return ApiResponse.ok(service.lowStock(com.hms.common.BranchContext.branchId()));
    }

    @GetMapping("/expiring")
    @PreAuthorize("hasAuthority('inventory.view')")
    public ApiResponse<List<InventoryService.BatchRow>> expiring(@RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok(service.expiring(com.hms.common.BranchContext.branchId(), days));
    }

    @PostMapping("/receive")
    @PreAuthorize("hasAuthority('inventory.receive')")
    public ApiResponse<InventoryService.ReceiveResult> receive(
            @RequestBody InventoryService.ReceiveRequest req) {
        return ApiResponse.ok(service.receive(req));
    }

    @PostMapping("/adjust")
    @PreAuthorize("hasAuthority('inventory.adjust')")
    public ApiResponse<Void> adjust(@RequestBody InventoryService.AdjustRequest req) {
        service.adjust(req);
        return ApiResponse.ok("Stock adjusted", null);
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAuthority('inventory.view')")
    public ApiResponse<PagedResponse<InventoryService.MovementResponse>> movements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(PagedResponse.of(service.movements(page, size)));
    }

    @GetMapping("/transfers")
    @PreAuthorize("hasAuthority('inventory.transfer')")
    public ApiResponse<PagedResponse<InventoryService.TransferResponse>> transfers(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(PagedResponse.of(service.listTransfers(status, page, size)));
    }

    @PostMapping("/transfers")
    @PreAuthorize("hasAuthority('inventory.transfer')")
    public ApiResponse<InventoryService.TransferResponse> requestTransfer(
            @RequestBody InventoryService.TransferRequest req) {
        return ApiResponse.ok(service.requestTransfer(req));
    }

    @PostMapping("/transfers/{id}/approve")
    @PreAuthorize("hasAuthority('inventory.transfer')")
    public ApiResponse<InventoryService.TransferResponse> approve(@PathVariable Long id) {
        return ApiResponse.ok(service.approveTransfer(id));
    }

    @PostMapping("/transfers/{id}/reject")
    @PreAuthorize("hasAuthority('inventory.transfer')")
    public ApiResponse<InventoryService.TransferResponse> reject(@PathVariable Long id,
                                                                 @RequestParam(required = false) String reason) {
        return ApiResponse.ok(service.rejectTransfer(id, reason));
    }

    @PostMapping("/transfers/{id}/ship")
    @PreAuthorize("hasAuthority('inventory.transfer')")
    public ApiResponse<InventoryService.TransferResponse> ship(@PathVariable Long id) {
        return ApiResponse.ok(service.shipTransfer(id));
    }

    @PostMapping("/transfers/{id}/receive")
    @PreAuthorize("hasAuthority('inventory.transfer')")
    public ApiResponse<InventoryService.TransferResponse> receive(@PathVariable Long id) {
        return ApiResponse.ok(service.receiveTransfer(id));
    }
}