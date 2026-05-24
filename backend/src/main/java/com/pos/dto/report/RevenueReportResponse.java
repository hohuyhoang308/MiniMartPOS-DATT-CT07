package com.pos.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Báo cáo doanh thu/lợi nhuận theo khoảng thời gian, gộp theo kỳ (FR9.2). */
public record RevenueReportResponse(
        LocalDate from,
        LocalDate to,
        String groupBy,                 // DAY | WEEK | MONTH | YEAR
        BigDecimal totalRevenue,
        BigDecimal totalProfit,
        long totalInvoices,
        List<PeriodPoint> points
) {
    /** Một điểm trên trục thời gian: nhãn kỳ + doanh thu + lợi nhuận + số HĐ. */
    public record PeriodPoint(String label, BigDecimal revenue, BigDecimal profit, Long invoiceCount) {}
}
