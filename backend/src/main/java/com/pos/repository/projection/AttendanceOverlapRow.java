package com.pos.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Ngày một nhân viên VỪA có ca VỪA có chấm công thủ công hưởng lương — nguy cơ tính trùng giờ. */
public interface AttendanceOverlapRow {
    String getFullName();
    LocalDate getWorkDate();
    BigDecimal getHours();   // tổng giờ chấm công thủ công hưởng lương trong ngày đó
}
