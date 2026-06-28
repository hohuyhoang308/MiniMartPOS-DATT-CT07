package com.pos.dto.payroll;

import com.pos.entity.PayslipAdjustment;
import com.pos.entity.enums.PayslipAdjustmentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PayslipAdjustmentResponse(
        Long id, PayslipAdjustmentType type, BigDecimal amount,
        String reason, LocalDateTime createdAt) {

    public static PayslipAdjustmentResponse from(PayslipAdjustment a) {
        return new PayslipAdjustmentResponse(
                a.getId(), a.getType(), a.getAmount(), a.getReason(), a.getCreatedAt());
    }
}
