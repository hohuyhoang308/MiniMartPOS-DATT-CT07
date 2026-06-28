package com.pos.repository.projection;

import java.math.BigDecimal;

/** Giờ công cộng dồn của một nhân viên trong kỳ: tổng giờ ca ĐÃ ĐÓNG + số ca. */
public interface WorkedHoursRow {
    Long getUserId();
    String getFullName();
    BigDecimal getWorkedHours();   // Σ (closed_at − opened_at) tính bằng GIỜ
    Long getShiftCount();
}
