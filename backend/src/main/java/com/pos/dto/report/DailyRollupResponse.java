package com.pos.dto.report;

import com.pos.entity.DailySalesRollup;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Một dòng tổng hợp doanh thu ngày đọc từ bảng rollup (Obj 2). */
public record DailyRollupResponse(
        Long storeId,
        LocalDate salesDate,
        BigDecimal revenue,
        BigDecimal grossProfit,
        BigDecimal cogs,
        BigDecimal tax,
        Integer invoiceCount,
        Integer itemsSold
) {
    public static DailyRollupResponse from(DailySalesRollup r) {
        return new DailyRollupResponse(r.getStoreId(), r.getSalesDate(), r.getRevenue(),
                r.getGrossProfit(), r.getCogs(), r.getTax(), r.getInvoiceCount(), r.getItemsSold());
    }
}
