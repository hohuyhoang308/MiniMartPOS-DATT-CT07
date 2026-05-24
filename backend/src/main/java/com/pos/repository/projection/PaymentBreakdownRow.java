package com.pos.repository.projection;

import java.math.BigDecimal;

/** Cơ cấu doanh thu theo hình thức thanh toán (CASH/QR). */
public interface PaymentBreakdownRow {
    String getMethod();
    Long getCnt();
    BigDecimal getAmount();
}
