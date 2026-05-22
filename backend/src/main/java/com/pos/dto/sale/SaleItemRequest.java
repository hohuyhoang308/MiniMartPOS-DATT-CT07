package com.pos.dto.sale;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SaleItemRequest(
        @NotNull(message = "Phải có sản phẩm") Long productId,
        @NotNull @Min(value = 1, message = "Số lượng phải ≥ 1") Integer quantity
) {}
