package com.pos.service.promotion;

import com.pos.entity.Promotion;
import com.pos.entity.enums.DiscountType;
import com.pos.util.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Giảm theo PHẦN TRĂM. VND không có hào → làm tròn về ĐỒNG (Money) tránh tiền lẻ không tiêu được. */
@Component
public class PercentDiscountStrategy implements DiscountStrategy {

    @Override
    public DiscountType type() {
        return DiscountType.PERCENT;
    }

    @Override
    public BigDecimal computeDiscount(Promotion promotion, BigDecimal subtotal) {
        return Money.round(subtotal.multiply(promotion.getDiscountValue()).movePointLeft(2));
    }
}
