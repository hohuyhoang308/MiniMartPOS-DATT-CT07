package com.pos.repository.projection;

import java.math.BigDecimal;

/** Giờ chấm công thủ công cộng dồn của một nhân viên trong kỳ (tách giờ hưởng lương & giờ nghỉ phép). */
public interface AttendanceHoursRow {
    Long getUserId();
    String getFullName();
    BigDecimal getPaidHours();        // Σ giờ WORK + LEAVE_PAID (tính vào lương)
    BigDecimal getLeavePaidHours();   // Σ giờ LEAVE_PAID (để hiển thị tách bạch)
}
