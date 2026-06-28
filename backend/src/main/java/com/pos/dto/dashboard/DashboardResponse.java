package com.pos.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

/** Dữ liệu tổng quan Dashboard chuyên nghiệp (FR9.1). */
public record DashboardResponse(
        // Doanh thu & lợi nhuận
        BigDecimal revenueToday,
        BigDecimal revenueYesterday,
        BigDecimal revenueMonth,
        BigDecimal profitToday,
        BigDecimal profitMonth,
        // Chi phí nhân sự: tổng quỹ lương tháng này & tỷ lệ trên doanh thu tháng (%)
        BigDecimal laborCostMonth,
        BigDecimal laborCostRatio,
        // Chỉ số bán hàng hôm nay
        long invoiceCountToday,
        long itemsSoldToday,
        long customersToday,
        BigDecimal avgOrderValue,
        // Cảnh báo kho
        long lowStockCount,
        long outOfStockCount,
        long expiringCount,
        // Biểu đồ & danh sách
        List<TopProduct> topProducts,
        List<DailyPoint> revenueChart,
        List<PaymentSlice> paymentBreakdown,
        List<HourPoint> hourlySales,
        List<CategorySlice> categorySales,
        List<RecentInvoice> recentInvoices
) {
    public record TopProduct(Long productId, String productName, Long quantitySold, BigDecimal revenue) {}
    public record DailyPoint(String day, BigDecimal revenue, Long invoiceCount) {}
    public record PaymentSlice(String method, Long count, BigDecimal amount) {}
    public record HourPoint(Integer hour, BigDecimal revenue, Long invoiceCount) {}
    public record CategorySlice(String categoryName, BigDecimal revenue, Long quantity) {}
    public record RecentInvoice(String code, BigDecimal totalAmount, String paymentMethod,
                                String status, String cashierName, String createdAt) {}
}
