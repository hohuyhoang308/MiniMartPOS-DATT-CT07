package com.pos.dto.shift;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OpenShiftRequest(
        @NotNull(message = "Phải nhập tiền đầu ca")
        @DecimalMin(value = "0", message = "Tiền đầu ca phải ≥ 0") BigDecimal openingCash
) {}
