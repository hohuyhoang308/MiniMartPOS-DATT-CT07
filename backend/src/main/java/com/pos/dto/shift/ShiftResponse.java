package com.pos.dto.shift;

import com.pos.entity.WorkShift;
import com.pos.entity.enums.ShiftStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Thông tin ca + doanh thu + ĐỐI SOÁT QUỸ.
 * <ul>
 *   <li>{@code cashSales}     — tiền mặt thực thu trong ca (HĐ CASH đã hoàn tất).</li>
 *   <li>{@code qrSales}       — doanh thu QR/CK trong ca (KHÔNG nằm trong két tiền mặt).</li>
 *   <li>{@code expectedCash}  — tiền mặt dự kiến trong két = tiền đầu ca + tiền mặt bán.</li>
 *   <li>{@code cashDifference}— chênh lệch khi đóng ca = tiền đếm thực (closingCash) − dự kiến
 *       (dương = thừa quỹ, âm = thiếu quỹ; null khi ca chưa đóng).</li>
 * </ul>
 */
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
        Long invoiceCount,
        BigDecimal cashSales,
        BigDecimal qrSales,
        BigDecimal cashRefunds,
        BigDecimal expectedCash,
        BigDecimal cashDifference
) {
    public static ShiftResponse from(WorkShift s, String cashierName,
                                     BigDecimal totalSales, Long invoiceCount, BigDecimal cashSales,
                                     BigDecimal cashRefunds) {
        BigDecimal opening = s.getOpeningCash() != null ? s.getOpeningCash() : BigDecimal.ZERO;
        BigDecimal total = totalSales != null ? totalSales : BigDecimal.ZERO;
        BigDecimal cash = cashSales != null ? cashSales : BigDecimal.ZERO;
        BigDecimal refunds = cashRefunds != null ? cashRefunds : BigDecimal.ZERO;
        BigDecimal qr = total.subtract(cash).max(BigDecimal.ZERO);  // doanh thu QR = tổng − tiền mặt
        // Tiền mặt dự kiến = đầu ca + tiền mặt bán − tiền hoàn trả (chi quỹ khi trả hàng).
        BigDecimal expected = opening.add(cash).subtract(refunds);
        BigDecimal diff = s.getClosingCash() != null ? s.getClosingCash().subtract(expected) : null;
        return new ShiftResponse(
                s.getId(), s.getUser().getId(), cashierName,
                opening, s.getClosingCash(),
                s.getOpenedAt(), s.getClosedAt(), s.getStatus(),
                total, invoiceCount != null ? invoiceCount : 0L,
                cash, qr, refunds, expected, diff);
    }
}
