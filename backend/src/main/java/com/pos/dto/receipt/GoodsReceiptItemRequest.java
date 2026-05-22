package com.pos.dto.receipt;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoodsReceiptItemRequest(
        @NotNull(message = "Phải chọn sản phẩm") Long productId,
        @NotNull @Min(value = 1, message = "Số lượng nhập phải ≥ 1") Integer quantity,
        @NotNull @DecimalMin(value = "0", message = "Giá nhập phải ≥ 0") BigDecimal importPrice,
        LocalDate expiryDate
) {}
