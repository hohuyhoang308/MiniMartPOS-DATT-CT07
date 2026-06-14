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
 *   <li>{@code cashIn/cashOut}— tổng THU/CHI tiền mặt NGOÀI bán hàng trong ca (petty cash).</li>
 *   <li>{@code expectedCash}  — tiền mặt dự kiến trong két = đầu ca + tiền mặt bán + thu − chi.</li>
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
        BigDecimal cashIn,
        BigDecimal cashOut,
        BigDecimal expectedCash,
        BigDecimal cashDifference
) {
    /** Overload tương thích (không có thu/chi ngoài bán hàng) — dùng ở nơi chưa tính petty cash/test. */
    public static ShiftResponse from(WorkShift s, String cashierName,
                                     BigDecimal totalSales, Long invoiceCount, BigDecimal cashSales) {
        return from(s, cashierName, totalSales, invoiceCount, cashSales, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public static ShiftResponse from(WorkShift s, String cashierName,
                                     BigDecimal totalSales, Long invoiceCount, BigDecimal cashSales,
                                     BigDecimal cashIn, BigDecimal cashOut) {
        BigDecimal opening = s.getOpeningCash() != null ? s.getOpeningCash() : BigDecimal.ZERO;
        BigDecimal total = totalSales != null ? totalSales : BigDecimal.ZERO;
        BigDecimal cash = cashSales != null ? cashSales : BigDecimal.ZERO;
        BigDecimal in = cashIn != null ? cashIn : BigDecimal.ZERO;
        BigDecimal out = cashOut != null ? cashOut : BigDecimal.ZERO;
        BigDecimal qr = total.subtract(cash).max(BigDecimal.ZERO);  // doanh thu QR = tổng − tiền mặt
        // Tiền mặt dự kiến = đầu ca + tiền mặt bán + thu ngoài − chi ngoài.
        BigDecimal expected = opening.add(cash).add(in).subtract(out);
        BigDecimal diff = s.getClosingCash() != null ? s.getClosingCash().subtract(expected) : null;
        return new ShiftResponse(
                s.getId(), s.getUser().getId(), cashierName,
                opening, s.getClosingCash(),
                s.getOpenedAt(), s.getClosedAt(), s.getStatus(),
                total, invoiceCount != null ? invoiceCount : 0L,
                cash, qr, in, out, expected, diff);
    }
}
