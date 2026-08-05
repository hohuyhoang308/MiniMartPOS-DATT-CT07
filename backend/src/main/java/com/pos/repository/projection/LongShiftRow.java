package com.pos.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Ca ĐÃ ĐÓNG có thời lượng dài bất thường (nghi quên đóng ca) — cảnh báo khi tính lương. */
public interface LongShiftRow {
    Long getShiftId();
    String getFullName();
    LocalDateTime getOpenedAt();
    BigDecimal getHours();   // thời lượng ca tính bằng GIỜ
}
