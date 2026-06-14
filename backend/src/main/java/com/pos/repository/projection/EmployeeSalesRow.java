package com.pos.repository.projection;

import java.math.BigDecimal;

/** Doanh số bán của một THU NGÂN trong khoảng — báo cáo hiệu suất nhân viên. */
public interface EmployeeSalesRow {
    Long getUserId();
    String getCashierName();
    Long getInvoiceCount();
    BigDecimal getRevenue();
}
