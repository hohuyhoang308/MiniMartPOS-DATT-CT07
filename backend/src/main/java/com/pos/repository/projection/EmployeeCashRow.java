package com.pos.repository.projection;

import java.math.BigDecimal;

/** Trách nhiệm quỹ của một thu ngân: tổng lệch quỹ + số ca (đã đóng) trong khoảng. */
public interface EmployeeCashRow {
    Long getUserId();
    BigDecimal getVariance();   // tổng (đếm thực − dự kiến) các ca đã đóng: âm = thiếu, dương = thừa
    Long getClosedShifts();
    Long getTotalShifts();
}
