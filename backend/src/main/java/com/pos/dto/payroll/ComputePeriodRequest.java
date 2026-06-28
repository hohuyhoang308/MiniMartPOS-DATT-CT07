package com.pos.dto.payroll;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Yêu cầu tạo/tính lại kỳ lương cho tháng {@code 'YYYY-MM'}. */
public record ComputePeriodRequest(
        @NotBlank(message = "Phải chọn tháng lương")
        @Pattern(regexp = "\\d{4}-\\d{2}", message = "Tháng lương phải có dạng YYYY-MM") String month
) {}
