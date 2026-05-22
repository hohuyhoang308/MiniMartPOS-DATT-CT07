package com.pos.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

/** Dữ liệu tổng quan Dashboard (FR9.1). */
public record DashboardResponse(
        BigDecimal revenueToday,
        BigDecimal revenueMonth,
        long invoiceCountToday,
        long lowStockCount,
        List<TopProduct> topProducts,
        List<DailyPoint> revenueChart
) {
    public record TopProduct(Long productId, String productName, Long quantitySold, BigDecimal revenue) {}
    public record DailyPoint(String day, BigDecimal revenue, Long invoiceCount) {}
}
