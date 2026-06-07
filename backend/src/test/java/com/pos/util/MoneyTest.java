package com.pos.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Kiểm thử chính sách tiền tệ VND (làm tròn về đồng, HALF_UP) — nền tảng mọi phép tính tiền. */
class MoneyTest {

    @Test
    void round_null_returns_zero() {
        assertThat(Money.round(null)).isEqualByComparingTo("0");
    }

    @Test
    void round_uses_half_up_to_dong() {
        assertThat(Money.round(new BigDecimal("1234.5"))).isEqualByComparingTo("1235");
        assertThat(Money.round(new BigDecimal("1234.4"))).isEqualByComparingTo("1234");
        assertThat(Money.round(new BigDecimal("2.5"))).isEqualByComparingTo("3");
        assertThat(Money.round(new BigDecimal("100"))).isEqualByComparingTo("100");
    }

    @Test
    void round_result_has_scale_zero() {
        assertThat(Money.round(new BigDecimal("9999.99")).scale()).isZero();
    }

    @Test
    void prorate_divides_then_rounds() {
        // 100 * 1 / 3 = 33.33… → 33
        assertThat(Money.prorate(new BigDecimal("100"), BigDecimal.ONE, new BigDecimal("3")))
                .isEqualByComparingTo("33");
        // 100 * 2 / 3 = 66.66… → 67
        assertThat(Money.prorate(new BigDecimal("100"), new BigDecimal("2"), new BigDecimal("3")))
                .isEqualByComparingTo("67");
    }

    @Test
    void prorate_scales_tax_to_amount_after_discount() {
        // Thuế gộp 10.000 trên tổng 100.000; sau giảm còn thực thu 90.000 → thuế co giãn 9.000
        BigDecimal grossTax = new BigDecimal("10000");
        BigDecimal total = new BigDecimal("90000");
        BigDecimal subtotal = new BigDecimal("100000");
        assertThat(Money.prorate(grossTax, total, subtotal)).isEqualByComparingTo("9000");
    }

    @Test
    void prorate_with_non_positive_denominator_just_rounds_amount() {
        assertThat(Money.prorate(new BigDecimal("123.6"), BigDecimal.TEN, BigDecimal.ZERO))
                .isEqualByComparingTo("124");
        assertThat(Money.prorate(new BigDecimal("123.6"), BigDecimal.TEN, new BigDecimal("-5")))
                .isEqualByComparingTo("124");
    }

    @Test
    void prorate_null_inputs_return_zero_amount() {
        assertThat(Money.prorate(null, BigDecimal.ONE, BigDecimal.TEN)).isEqualByComparingTo("0");
        assertThat(Money.prorate(new BigDecimal("100"), null, BigDecimal.TEN)).isEqualByComparingTo("100");
    }
}
