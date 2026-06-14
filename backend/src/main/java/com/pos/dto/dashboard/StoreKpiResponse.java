package com.pos.dto.dashboard;

import com.pos.entity.enums.CommonStatus;

import java.math.BigDecimal;

/**
 * KPI tóm tắt của MỘT chi nhánh — phục vụ màn "So sánh chi nhánh" của quản trị viên toàn chuỗi (ADMIN).
 * Mỗi dòng là một cửa hàng; cho phép xếp hạng/so sánh doanh thu, lợi nhuận, số đơn và cảnh báo tồn kho.
 */
public record StoreKpiResponse(
        Long storeId,
        String code,
        String name,
        CommonStatus status,
        BigDecimal revenueToday,
        BigDecimal revenueMonth,
        BigDecimal profitMonth,
        long invoicesToday,
        long lowStock,
        long outOfStock) {
}
