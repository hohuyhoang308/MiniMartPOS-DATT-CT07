package com.pos.repository.projection;

import java.math.BigDecimal;

/** Dòng doanh thu theo ngày (native query). */
public interface DailyRevenueRow {
    String getDay();
    BigDecimal getRevenue();
    Long getInvoiceCount();
}
