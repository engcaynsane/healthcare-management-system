package com.hms.report;

import com.hms.appointment.AppointmentRepository;
import com.hms.common.BranchContext;
import com.hms.inventory.InventoryService;
import com.hms.lab.LabOrderRepository;
import com.hms.patient.PatientRepository;
import com.hms.sale.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SaleRepository saleRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final LabOrderRepository labOrderRepository;
    private final InventoryService inventoryService;

    public Map<String, Object> summary() {
        Long branchId = BranchContext.branchId();
        LocalDate today = LocalDate.now();
        long salesToday = saleRepository.countForDate(branchId, today);
        BigDecimal revenueToday = saleRepository.sumForDate(branchId, today);
        long patientsToday = patientRepository.countByBranchAndDate(branchId, today);
        long appointmentsToday = appointmentRepository.countToday(branchId, today);
        long pendingLabs = labOrderRepository.countPending(branchId);
        int lowStock = inventoryService.lowStock(branchId).size();
        int expiringSoon = inventoryService.expiring(branchId, 30).size();
        int pendingTransfers = inventoryService.pendingTransfers(branchId);

        return Map.of(
                "salesToday", salesToday,
                "revenueToday", revenueToday,
                "patientsToday", patientsToday,
                "appointmentsToday", appointmentsToday,
                "pendingLabs", pendingLabs,
                "lowStock", lowStock,
                "expiringSoon", expiringSoon,
                "pendingTransfers", pendingTransfers);
    }
}