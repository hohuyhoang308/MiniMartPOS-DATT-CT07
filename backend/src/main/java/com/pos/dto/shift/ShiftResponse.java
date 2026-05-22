package com.pos.dto.shift;

import com.pos.entity.WorkShift;
import com.pos.entity.enums.ShiftStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Thông tin ca + doanh thu (suy ra từ view v_shift_summary). */
public record ShiftResponse(
        Long id,
        Long userId,
        String cashierName,
        BigDecimal openingCash,
        BigDecimal closingCash,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        ShiftStatus status,
        BigDecimal totalSales,
        Long invoiceCount
) {
    public static ShiftResponse from(WorkShift s, String cashierName,
                                     BigDecimal totalSales, Long invoiceCount) {
        return new ShiftResponse(
                s.getId(), s.getUser().getId(), cashierName,
                s.getOpeningCash(), s.getClosingCash(),
                s.getOpenedAt(), s.getClosedAt(), s.getStatus(),
                totalSales != null ? totalSales : BigDecimal.ZERO,
                invoiceCount != null ? invoiceCount : 0L);
    }
}
