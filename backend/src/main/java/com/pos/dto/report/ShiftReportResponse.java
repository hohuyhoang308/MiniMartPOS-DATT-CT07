package com.pos.dto.report;

import com.pos.entity.view.ShiftSummaryView;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Báo cáo theo ca/thu ngân kèm đối soát quỹ tiền mặt (FR9.2).
 *  {@code cashMovement} = RÒNG thu/chi tiền mặt ngoài bán hàng (THU − CHI). */
public record ShiftReportResponse(
        Long shiftId, String cashierName, BigDecimal openingCash, BigDecimal closingCash,
        LocalDateTime openedAt, LocalDateTime closedAt, String status,
        BigDecimal totalSales, Long invoiceCount,
        BigDecimal cashSales, BigDecimal cashMovement, BigDecimal expectedCash, BigDecimal cashDifference) {

    /** Overload tương thích (chưa tính thu/chi ngoài bán hàng). */
    public static ShiftReportResponse from(ShiftSummaryView v, BigDecimal cashSales) {
        return from(v, cashSales, BigDecimal.ZERO);
    }

    public static ShiftReportResponse from(ShiftSummaryView v, BigDecimal cashSales, BigDecimal netCashMovement) {
        BigDecimal opening = v.getOpeningCash() != null ? v.getOpeningCash() : BigDecimal.ZERO;
        BigDecimal cash = cashSales != null ? cashSales : BigDecimal.ZERO;
        BigDecimal net = netCashMovement != null ? netCashMovement : BigDecimal.ZERO;
        // Tiền mặt dự kiến = đầu ca + tiền mặt bán + ròng thu/chi ngoài bán hàng.
        BigDecimal expected = opening.add(cash).add(net);
        BigDecimal diff = v.getClosingCash() != null ? v.getClosingCash().subtract(expected) : null;
        return new ShiftReportResponse(v.getShiftId(), v.getCashierName(), opening,
                v.getClosingCash(), v.getOpenedAt(), v.getClosedAt(), v.getStatus(),
                v.getTotalSales(), v.getInvoiceCount(), cash, net, expected, diff);
    }
}
