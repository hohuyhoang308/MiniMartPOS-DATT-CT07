package com.pos.dto.report;

import java.math.BigDecimal;

/**
 * Hiệu suất một THU NGÂN trong khoảng — để quản lý theo dõi nhân viên:
 * doanh số bán, số hóa đơn, số hàng bán, giá trị TB/HĐ, số ca, và TỔNG LỆCH QUỸ (trách nhiệm tiền mặt).
 */
public record EmployeeReportResponse(
        Long userId,
        String cashierName,
        long invoiceCount,
        BigDecimal revenue,
        long itemsSold,
        BigDecimal avgOrderValue,
        long totalShifts,
        long closedShifts,
        BigDecimal cashVariance   // âm = thiếu quỹ tích lũy; dương = thừa
) {}
