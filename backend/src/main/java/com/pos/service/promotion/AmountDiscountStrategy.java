package com.pos.service.promotion;

import com.pos.entity.Promotion;
import com.pos.entity.enums.DiscountType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Giảm theo SỐ TIỀN cố định. */
@Component
public class AmountDiscountStrategy implements DiscountStrategy {

    @Override
    public DiscountType type() {
        return DiscountType.AMOUNT;
    }

    @Override
    public BigDecimal computeDiscount(Promotion promotion, BigDecimal subtotal) {
        return promotion.getDiscountValue();
    }
}
