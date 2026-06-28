package com.pos.dto.payroll;

import com.pos.entity.Payslip;
import com.pos.entity.enums.PayType;

import java.math.BigDecimal;
import java.util.List;

/** Phiếu lương đầy đủ (snapshot công + tiền) + các dòng thưởng/phạt. */
public record PayslipResponse(
        Long id, Long userId, String fullName,
        PayType payType, BigDecimal baseRate, BigDecimal standardHours,
        BigDecimal workedHours, BigDecimal regularHours, BigDecimal otHours, int shiftCount,
        BigDecimal regularPay, BigDecimal otPay, BigDecimal allowance, BigDecimal grossPay,
        BigDecimal totalBonus, BigDecimal totalDeduction, BigDecimal netPay,
        List<PayslipAdjustmentResponse> adjustments) {

    public static PayslipResponse from(Payslip p, String fullName, List<PayslipAdjustmentResponse> adjustments) {
        return new PayslipResponse(
                p.getId(), p.getUser().getId(), fullName,
                p.getPayType(), p.getBaseRate(), p.getStandardHours(),
                p.getWorkedHours(), p.getRegularHours(), p.getOtHours(), p.getShiftCount(),
                p.getRegularPay(), p.getOtPay(), p.getAllowance(), p.getGrossPay(),
                p.getTotalBonus(), p.getTotalDeduction(), p.getNetPay(),
                adjustments);
    }
}
