package com.hms.lab;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LabOrderItemRepository extends JpaRepository<LabOrderItem, Long> {

    List<LabOrderItem> findByLabOrderId(Long labOrderId);
}