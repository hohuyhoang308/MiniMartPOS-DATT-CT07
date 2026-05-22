package com.pos.dto.report;

import com.pos.entity.view.ShiftSummaryView;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Báo cáo theo ca/thu ngân (FR9.2). */
public record ShiftReportResponse(
        Long shiftId, String cashierName, BigDecimal openingCash, BigDecimal closingCash,
        LocalDateTime openedAt, LocalDateTime closedAt, String status,
        BigDecimal totalSales, Long invoiceCount) {

    public static ShiftReportResponse from(ShiftSummaryView v) {
        return new ShiftReportResponse(v.getShiftId(), v.getCashierName(), v.getOpeningCash(),
                v.getClosingCash(), v.getOpenedAt(), v.getClosedAt(), v.getStatus(),
                v.getTotalSales(), v.getInvoiceCount());
    }
}
