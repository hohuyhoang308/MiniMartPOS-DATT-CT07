package com.pos.dto.shift;

import com.pos.entity.enums.CashMovementType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Yêu cầu ghi một khoản THU/CHI tiền mặt vào ca đang mở của thu ngân hiện tại. */
public record CashMovementRequest(
        @NotNull(message = "Phải chọn loại (thu/chi)") CashMovementType type,
        @NotNull(message = "Phải nhập số tiền")
        @DecimalMin(value = "1", message = "Số tiền phải lớn hơn 0") BigDecimal amount,
        @NotBlank(message = "Phải nhập lý do") @Size(max = 255) String reason
) {}
