package com.pos.repository.projection;

import java.math.BigDecimal;

/** Dòng báo cáo doanh thu/lợi nhuận theo kỳ (ngày/tuần/tháng/năm) — native query. */
public interface PeriodReportRow {
    String getBucket();
    BigDecimal getRevenue();
    BigDecimal getProfit();
    Long getInvoiceCount();
}
