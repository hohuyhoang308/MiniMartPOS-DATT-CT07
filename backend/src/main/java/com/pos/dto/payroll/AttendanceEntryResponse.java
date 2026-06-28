package com.pos.dto.payroll;

import com.pos.entity.AttendanceEntry;
import com.pos.entity.enums.AttendanceType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AttendanceEntryResponse(
        Long id, Long userId, String fullName,
        LocalDate workDate, AttendanceType type, BigDecimal hours, String reason) {

    public static AttendanceEntryResponse from(AttendanceEntry a) {
        return new AttendanceEntryResponse(
                a.getId(), a.getUser().getId(), a.getUser().getFullName(),
                a.getWorkDate(), a.getType(), a.getHours(), a.getReason());
    }
}
