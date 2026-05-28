package com.pos.dto.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Yêu cầu lấy hàng của một LÔ từ kệ về lại kho. */
public record ShelfReturnRequest(
        @NotNull(message = "Phải chọn lô") Long batchId,
        @NotNull @Min(value = 1, message = "Số lượng phải ≥ 1") Integer quantity
) {}
