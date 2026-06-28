package com.pos.dto.payroll;

import com.pos.entity.enums.PayType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Đặt/cập nhật cấu hình lương một nhân viên. */
public record PayProfileRequest(
        @NotNull(message = "Phải chọn loại lương") PayType payType,
        @NotNull(message = "Phải nhập đơn giá lương")
        @DecimalMin(value = "0", message = "Đơn giá lương không được âm") BigDecimal baseRate,
        @NotNull(message = "Phải nhập công chuẩn/tháng")
        @DecimalMin(value = "1", message = "Công chuẩn/tháng phải lớn hơn 0") BigDecimal standardMonthlyHours,
        @NotNull(message = "Phải nhập hệ số tăng ca")
        @DecimalMin(value = "1", message = "Hệ số tăng ca tối thiểu là 1") BigDecimal otMultiplier,
        @NotNull(message = "Phải nhập phụ cấp")
        @DecimalMin(value = "0", message = "Phụ cấp không được âm") BigDecimal monthlyAllowance
) {}
