package com.pos.dto.promotion;

import com.pos.entity.enums.CommonStatus;
import com.pos.entity.enums.DiscountType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PromotionRequest(
        @NotBlank(message = "Mã khuyến mãi không được để trống") @Size(max = 30) String code,
        @NotBlank(message = "Tên chương trình không được để trống") @Size(max = 150) String name,
        @NotNull(message = "Phải chọn loại giảm") DiscountType discountType,
        @NotNull @DecimalMin(value = "0", message = "Giá trị giảm phải ≥ 0") BigDecimal discountValue,
        @DecimalMin(value = "0", message = "Đơn tối thiểu phải ≥ 0") BigDecimal minOrderAmount,
        @NotNull(message = "Phải có ngày bắt đầu") LocalDateTime startDate,
        @NotNull(message = "Phải có ngày kết thúc") LocalDateTime endDate,
        @Min(value = 1, message = "Giới hạn lượt phải ≥ 1") Integer usageLimit,
        CommonStatus status
) {}
