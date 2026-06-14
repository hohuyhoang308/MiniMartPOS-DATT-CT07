package com.pos.repository.projection;

import java.math.BigDecimal;

/** Tiền mặt thực thu của một ca — gộp theo nhiều ca để tránh N+1 ở báo cáo. */
public interface ShiftCashRow {
    Long getShiftId();
    BigDecimal getAmount();
}
