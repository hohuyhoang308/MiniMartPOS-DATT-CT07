package com.pos.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Báo cáo doanh thu theo khoảng thời gian (FR9.2). */
public record RevenueReportResponse(
        LocalDate from,
        LocalDate to,
        BigDecimal totalRevenue,
        long totalInvoices,
        List<DailyPoint> days
) {
    public record DailyPoint(String day, BigDecimal revenue, Long invoiceCount) {}
}
