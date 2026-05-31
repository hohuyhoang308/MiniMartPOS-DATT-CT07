package com.pos.dto.returns;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Yêu cầu trả hàng cho một hóa đơn (trả từng phần theo dòng). */
public record CreateReturnRequest(
        @NotNull(message = "Thiếu hóa đơn") Long invoiceId,
        @NotNull @Size(min = 3, max = 255, message = "Lý do trả 3–255 ký tự") String reason,
        @NotEmpty(message = "Chọn ít nhất 1 dòng để trả") @Valid List<Line> items
) {
    public record Line(
            @NotNull Long invoiceItemId,
            @NotNull @Min(value = 1, message = "Số lượng trả ≥ 1") Integer quantity
    ) {}
}
