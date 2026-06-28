package com.pos.repository;

import com.pos.entity.PayslipAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayslipAdjustmentRepository extends JpaRepository<PayslipAdjustment, Long> {

    List<PayslipAdjustment> findByPayslipIdOrderByCreatedAt(Long payslipId);

    List<PayslipAdjustment> findByPayslipIdInOrderByCreatedAt(List<Long> payslipIds);
}
