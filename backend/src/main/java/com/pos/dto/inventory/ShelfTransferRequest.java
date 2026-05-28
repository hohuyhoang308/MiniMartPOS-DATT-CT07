package com.pos.dto.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Yêu cầu đưa hàng từ kho lên một KỆ cụ thể. */
public record ShelfTransferRequest(
        @NotNull(message = "Phải chọn sản phẩm") Long productId,
        @NotNull(message = "Phải chọn kệ") Long shelfId,
        @NotNull @Min(value = 1, message = "Số lượng phải ≥ 1") Integer quantity
) {}
