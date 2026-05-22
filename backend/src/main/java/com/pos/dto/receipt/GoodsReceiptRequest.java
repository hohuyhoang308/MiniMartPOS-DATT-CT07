package com.pos.dto.receipt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record GoodsReceiptRequest(
        @NotNull(message = "Phải chọn nhà cung cấp") Long supplierId,
        @Size(max = 255) String note,
        /** true = cập nhật giá vốn sản phẩm theo giá nhập của lô này. */
        Boolean updateCostPrice,
        @NotEmpty(message = "Phiếu nhập phải có ít nhất 1 dòng")
        @Valid List<GoodsReceiptItemRequest> items
) {}
