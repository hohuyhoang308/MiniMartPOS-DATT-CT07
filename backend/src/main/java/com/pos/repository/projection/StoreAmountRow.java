package com.pos.repository.projection;

import java.math.BigDecimal;

/** Tổng một đại lượng tiền (doanh thu / COGS) gộp theo CHI NHÁNH — cho màn so sánh chi nhánh. */
public interface StoreAmountRow {
    Long getStoreId();
    BigDecimal getAmount();
}
