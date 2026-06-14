package com.pos.dto.transfer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/** Lập phiếu điều chuyển từ chi nhánh ĐANG XEM (nguồn) sang {@code destStoreId} (đích). */
public record CreateTransferRequest(
        @NotNull Long destStoreId,
        String note,
        @NotEmpty @Valid List<Line> items
) {
    public record Line(
            @NotNull Long batchId,        // lô nguồn (goods_receipt_items.id)
            @NotNull @Positive Integer quantity) {}
}
