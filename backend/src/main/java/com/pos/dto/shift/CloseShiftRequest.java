package com.pos.dto.shift;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CloseShiftRequest(
        @NotNull(message = "Phải nhập tiền đối soát cuối ca")
        @DecimalMin(value = "0", message = "Tiền cuối ca phải ≥ 0") BigDecimal closingCash
) {}
