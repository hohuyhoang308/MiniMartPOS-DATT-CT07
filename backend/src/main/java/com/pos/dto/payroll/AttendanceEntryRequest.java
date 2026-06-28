package com.pos.dto.payroll;

import com.pos.entity.enums.AttendanceType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Thêm một bản ghi chấm công thủ công / nghỉ phép cho một nhân viên. */
public record AttendanceEntryRequest(
        @NotNull(message = "Phải chọn nhân viên") Long userId,
        @NotNull(message = "Phải chọn ngày") LocalDate workDate,
        @NotNull(message = "Phải chọn loại chấm công") AttendanceType type,
        @NotNull(message = "Phải nhập số giờ")
        @DecimalMin(value = "0.25", message = "Số giờ phải lớn hơn 0")
        @DecimalMax(value = "24", message = "Số giờ trong ngày không quá 24") BigDecimal hours,
        @Size(max = 255) String reason
) {}
