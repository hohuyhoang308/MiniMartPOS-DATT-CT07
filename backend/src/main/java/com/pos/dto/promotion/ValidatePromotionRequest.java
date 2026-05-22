package com.pos.dto.promotion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ValidatePromotionRequest(
        @NotBlank(message = "Phải nhập mã giảm giá") String code,
        @NotNull(message = "Phải có tổng tạm tính") BigDecimal subtotal
) {}
