package com.pos.repository.projection;

import java.math.BigDecimal;

/** Doanh thu theo từng giờ trong ngày (tìm giờ cao điểm). */
public interface HourlySalesRow {
    Integer getHour();
    BigDecimal getRevenue();
    Long getInvoiceCount();
}
