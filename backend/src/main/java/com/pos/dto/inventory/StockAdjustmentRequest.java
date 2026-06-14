package com.pos.dto.inventory;

import com.pos.entity.enums.AdjustmentReason;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Yêu cầu XUẤT HỦY / GIẢM TỒN một lô (rút {@code quantity} đơn vị khỏi kho của lô). */
public record StockAdjustmentRequest(
        @NotNull(message = "Phải chọn lô hàng") Long batchId,
        @NotNull(message = "Phải nhập số lượng") @Min(value = 1, message = "Số lượng phải lớn hơn 0") Integer quantity,
        @NotNull(message = "Phải chọn lý do") AdjustmentReason reason,
        @Size(max = 255) String note
) {}
