package com.pos.dto.payroll;

import com.pos.entity.PayrollPeriod;
import com.pos.entity.enums.PayrollStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Kỳ lương + (tùy chọn) danh sách phiếu lương khi xem chi tiết.
 * {@code payslips} = null ở danh sách kỳ; có dữ liệu ở API chi tiết.
 */
public record PayrollPeriodResponse(
        Long id, Long storeId, String storeName, String periodMonth,
        PayrollStatus status, String note,
        LocalDateTime createdAt, LocalDateTime submittedAt, LocalDateTime approvedAt, LocalDateTime paidAt,
        int employeeCount, BigDecimal totalNet,
        List<PayslipResponse> payslips) {

    public static PayrollPeriodResponse summary(PayrollPeriod p, int employeeCount, BigDecimal totalNet) {
        return new PayrollPeriodResponse(
                p.getId(),
                p.getStore().getId(), p.getStore().getName(),
                p.getPeriodMonth(), p.getStatus(), p.getNote(),
                p.getCreatedAt(), p.getSubmittedAt(), p.getApprovedAt(), p.getPaidAt(),
                employeeCount, totalNet, null);
    }

    public static PayrollPeriodResponse detail(PayrollPeriod p, List<PayslipResponse> payslips) {
        BigDecimal totalNet = payslips.stream().map(PayslipResponse::netPay)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PayrollPeriodResponse(
                p.getId(),
                p.getStore().getId(), p.getStore().getName(),
                p.getPeriodMonth(), p.getStatus(), p.getNote(),
                p.getCreatedAt(), p.getSubmittedAt(), p.getApprovedAt(), p.getPaidAt(),
                payslips.size(), totalNet, payslips);
    }
}
