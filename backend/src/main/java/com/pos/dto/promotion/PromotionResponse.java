package com.pos.dto.promotion;

import com.pos.entity.Promotion;
import com.pos.entity.enums.CommonStatus;
import com.pos.entity.enums.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PromotionResponse(
        Long id, String code, String name, DiscountType discountType,
        BigDecimal discountValue, BigDecimal minOrderAmount,
        LocalDateTime startDate, LocalDateTime endDate,
        Integer usageLimit, Integer usedCount, CommonStatus status) {

    public static PromotionResponse from(Promotion p) {
        return new PromotionResponse(p.getId(), p.getCode(), p.getName(), p.getDiscountType(),
                p.getDiscountValue(), p.getMinOrderAmount(), p.getStartDate(), p.getEndDate(),
                p.getUsageLimit(), p.getUsedCount(), p.getStatus());
    }
}
