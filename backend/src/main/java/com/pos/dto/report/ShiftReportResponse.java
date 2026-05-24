package com.pos.dto.report;

import com.pos.entity.view.ShiftSummaryView;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Báo cáo theo ca/thu ngân kèm đối soát quỹ tiền mặt (FR9.2). */
public record ShiftReportResponse(
        Long shiftId, String cashierName, BigDecimal openingCash, BigDecimal closingCash,
        LocalDateTime openedAt, LocalDateTime closedAt, String status,
        BigDecimal totalSales, Long invoiceCount,
        BigDecimal cashSales, BigDecimal expectedCash, BigDecimal cashDifference) {

    public static ShiftReportResponse from(ShiftSummaryView v, BigDecimal cashSales) {
        BigDecimal opening = v.getOpeningCash() != null ? v.getOpeningCash() : BigDecimal.ZERO;
        BigDecimal cash = cashSales != null ? cashSales : BigDecimal.ZERO;
        BigDecimal expected = opening.add(cash);
        BigDecimal diff = v.getClosingCash() != null ? v.getClosingCash().subtract(expected) : null;
        return new ShiftReportResponse(v.getShiftId(), v.getCashierName(), opening,
                v.getClosingCash(), v.getOpenedAt(), v.getClosedAt(), v.getStatus(),
                v.getTotalSales(), v.getInvoiceCount(), cash, expected, diff);
    }
}
