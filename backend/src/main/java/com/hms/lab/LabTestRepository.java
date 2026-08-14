package com.hms.lab;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LabTestRepository extends JpaRepository<LabTest, Long> {

    Optional<LabTest> findByCode(String code);

    List<LabTest> findByActiveTrueOrderByName();
}