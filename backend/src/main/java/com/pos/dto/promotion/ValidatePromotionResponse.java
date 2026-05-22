package com.pos.dto.promotion;

import java.math.BigDecimal;

/** Kết quả kiểm tra & tính giảm giá khi áp mã (FR7.2, UC11). */
public record ValidatePromotionResponse(
        Long promotionId,
        String code,
        String name,
        BigDecimal discountAmount,
        BigDecimal finalAmount
) {}
