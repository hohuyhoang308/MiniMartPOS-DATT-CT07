package com.pos.dto.payroll;

import com.pos.entity.enums.PayslipAdjustmentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Thêm dòng thưởng/phạt vào một phiếu lương. */
public record PayslipAdjustmentRequest(
        @NotNull(message = "Phải chọn loại điều chỉnh") PayslipAdjustmentType type,
        @NotNull(message = "Phải nhập số tiền")
        @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0") BigDecimal amount,
        @NotBlank(message = "Phải nhập lý do") @Size(max = 255) String reason
) {}
