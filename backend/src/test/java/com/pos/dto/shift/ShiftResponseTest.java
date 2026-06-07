package com.pos.dto.shift;

import com.pos.entity.User;
import com.pos.entity.WorkShift;
import com.pos.entity.enums.ShiftStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm thử logic ĐỐI SOÁT QUỸ CA (thuần tính toán trên record):
 * tiền mặt dự kiến = đầu ca + tiền mặt bán − hoàn trả; chênh lệch = đếm thực − dự kiến.
 */
class ShiftResponseTest {

    private WorkShift shift(BigDecimal opening, BigDecimal closing, ShiftStatus status) {
        User u = new User();
        u.setId(7L);
        WorkShift s = new WorkShift();
        s.setUser(u);
        s.setOpeningCash(opening);
        s.setClosingCash(closing);
        s.setStatus(status);
        return s;
    }

    @Test
    void expected_cash_is_opening_plus_cash_minus_refunds() {
        WorkShift s = shift(new BigDecimal("500000"), null, ShiftStatus.OPEN);

        ShiftResponse r = ShiftResponse.from(s, "Thu ngân A",
                new BigDecimal("400000"),   // tổng doanh thu
                3L,
                new BigDecimal("300000"),   // tiền mặt bán
                new BigDecimal("50000"));   // hoàn trả tiền mặt

        // 500.000 + 300.000 − 50.000
        assertThat(r.expectedCash()).isEqualByComparingTo("750000");
    }

    @Test
    void qr_sales_is_total_minus_cash() {
        WorkShift s = shift(new BigDecimal("500000"), null, ShiftStatus.OPEN);

        ShiftResponse r = ShiftResponse.from(s, "A",
                new BigDecimal("400000"), 3L,
                new BigDecimal("300000"), BigDecimal.ZERO);

        assertThat(r.qrSales()).isEqualByComparingTo("100000");
    }

    @Test
    void qr_sales_never_negative() {
        WorkShift s = shift(BigDecimal.ZERO, null, ShiftStatus.OPEN);

        ShiftResponse r = ShiftResponse.from(s, "A",
                new BigDecimal("100000"), 1L,
                new BigDecimal("150000"), BigDecimal.ZERO); // tiền mặt > tổng (bất thường)

        assertThat(r.qrSales()).isEqualByComparingTo("0");
    }

    @Test
    void cash_difference_is_null_while_shift_open() {
        WorkShift s = shift(new BigDecimal("500000"), null, ShiftStatus.OPEN);

        ShiftResponse r = ShiftResponse.from(s, "A",
                new BigDecimal("400000"), 3L,
                new BigDecimal("300000"), BigDecimal.ZERO);

        assertThat(r.cashDifference()).isNull();
    }

    @Test
    void cash_difference_zero_when_count_matches_expected() {
        // dự kiến = 500.000 + 300.000 − 50.000 = 750.000; đếm đúng 750.000
        WorkShift s = shift(new BigDecimal("500000"), new BigDecimal("750000"), ShiftStatus.CLOSED);

        ShiftResponse r = ShiftResponse.from(s, "A",
                new BigDecimal("400000"), 3L,
                new BigDecimal("300000"), new BigDecimal("50000"));

        assertThat(r.cashDifference()).isEqualByComparingTo("0");
    }

    @Test
    void cash_difference_positive_when_surplus_negative_when_short() {
        WorkShift surplus = shift(new BigDecimal("500000"), new BigDecimal("760000"), ShiftStatus.CLOSED);
        ShiftResponse r1 = ShiftResponse.from(surplus, "A",
                new BigDecimal("300000"), 1L, new BigDecimal("300000"), BigDecimal.ZERO);
        // dự kiến 800.000, đếm 760.000 → thiếu 40.000
        assertThat(r1.cashDifference()).isEqualByComparingTo("-40000");

        WorkShift short_ = shift(new BigDecimal("500000"), new BigDecimal("820000"), ShiftStatus.CLOSED);
        ShiftResponse r2 = ShiftResponse.from(short_, "A",
                new BigDecimal("300000"), 1L, new BigDecimal("300000"), BigDecimal.ZERO);
        // dự kiến 800.000, đếm 820.000 → thừa 20.000
        assertThat(r2.cashDifference()).isEqualByComparingTo("20000");
    }

    @Test
    void null_money_inputs_default_to_zero() {
        WorkShift s = shift(null, null, ShiftStatus.OPEN);

        ShiftResponse r = ShiftResponse.from(s, "A", null, null, null, null);

        assertThat(r.openingCash()).isEqualByComparingTo("0");
        assertThat(r.cashSales()).isEqualByComparingTo("0");
        assertThat(r.expectedCash()).isEqualByComparingTo("0");
        assertThat(r.invoiceCount()).isZero();
    }
}
