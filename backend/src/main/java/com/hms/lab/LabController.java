package com.hms.lab;

import com.hms.common.ApiResponse;
import com.hms.common.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lab")
@RequiredArgsConstructor
public class LabController {

    private final LabService labService;
    private final LabTestRepository labTestRepository;

    @GetMapping("/tests")
    @PreAuthorize("hasAuthority('lab.view')")
    public ApiResponse<List<LabTest>> tests() {
        return ApiResponse.ok(labTestRepository.findByActiveTrueOrderByName());
    }

    @PostMapping("/tests")
    @PreAuthorize("hasAuthority('lab.view')")
    public ApiResponse<LabTest> createTest(@RequestBody LabService.LabTestRequest req) {
        return ApiResponse.ok(labService.createTest(req));
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAuthority('lab.view')")
    public ApiResponse<PagedResponse<LabService.LabOrderSummary>> orders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(PagedResponse.of(labService.listOrders(status, page, size)));
    }

    @GetMapping("/orders/{id}")
    @PreAuthorize("hasAuthority('lab.view')")
    public ApiResponse<LabService.LabOrderDetail> orderDetail(@PathVariable Long id) {
        return ApiResponse.ok(labService.orderDetail(id));
    }

    @PostMapping("/orders")
    @PreAuthorize("hasAuthority('lab.order')")
    public ApiResponse<LabService.LabOrderDetail> createOrder(@RequestBody LabService.OrderRequest req) {
        return ApiResponse.ok(labService.orderDetail(labService.createOrder(req).getId()));
    }

    @PostMapping("/orders/{id}/result")
    @PreAuthorize("hasAuthority('lab.result')")
    public ApiResponse<LabService.LabOrderItemView> enterResult(@PathVariable Long id,
                                                                @RequestBody LabService.ResultRequest req) {
        return ApiResponse.ok(labService.enterResult(id, req));
    }

    @PostMapping("/orders/{id}/complete")
    @PreAuthorize("hasAuthority('lab.result')")
    public ApiResponse<LabService.LabOrderDetail> complete(@PathVariable Long id) {
        return ApiResponse.ok(labService.orderDetail(labService.completeOrder(id).getId()));
    }
}