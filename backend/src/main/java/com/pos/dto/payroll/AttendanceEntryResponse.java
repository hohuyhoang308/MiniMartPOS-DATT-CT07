package com.pos.dto.payroll;

import com.pos.entity.AttendanceEntry;
import com.pos.entity.enums.AttendanceType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AttendanceEntryResponse(
        Long id, Long userId, String fullName,
        LocalDate workDate, AttendanceType type, BigDecimal hours, String reason,
        String warning) {

    public static AttendanceEntryResponse from(AttendanceEntry a) {
        return from(a, null);
    }

    /** Kèm cảnh báo nghiệp vụ (vd ngày đó nhân viên đã có ca — nguy cơ tính trùng giờ). */
    public static AttendanceEntryResponse from(AttendanceEntry a, String warning) {
        return new AttendanceEntryResponse(
                a.getId(), a.getUser().getId(), a.getUser().getFullName(),
                a.getWorkDate(), a.getType(), a.getHours(), a.getReason(), warning);
    }
}
