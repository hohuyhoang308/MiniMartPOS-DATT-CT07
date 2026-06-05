package com.pos.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Chính sách TIỀN TỆ thống nhất cho VND.
 * Đồng Việt Nam KHÔNG có hào nên mọi số tiền đều làm tròn về ĐỒNG (scale 0) với {@link RoundingMode#HALF_UP}.
 * Dùng chung ở mọi nơi tính tiền (thuế, giảm giá, hoàn trả…) để tránh mỗi chỗ làm tròn một kiểu (HALF_UP/FLOOR,
 * scale 0/2) gây lệch số khó truy.
 */
public final class Money {

    private Money() {}

    public static final RoundingMode MODE = RoundingMode.HALF_UP;

    /** Làm tròn một số tiền VND về đồng (scale 0, HALF_UP). null ⇒ 0. */
    public static BigDecimal round(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(0, MODE);
    }

    /**
     * Chia theo tỉ lệ rồi làm tròn về đồng: {@code amount * numerator / denominator}.
     * Tiện cho hoàn tiền theo tỉ lệ tiền thực trả, co giãn thuế theo phần sau giảm giá…
     * Mẫu số ≤ 0 ⇒ trả về {@code round(amount)} (không chia).
     */
    public static BigDecimal prorate(BigDecimal amount, BigDecimal numerator, BigDecimal denominator) {
        if (amount == null || numerator == null || denominator == null || denominator.signum() <= 0) {
            return round(amount);
        }
        return amount.multiply(numerator).divide(denominator, 0, MODE);
    }
}
