package com.pos.repository.projection;

import java.math.BigDecimal;

/** Dòng "sản phẩm bán chạy" cho dashboard/báo cáo. */
public interface TopProductRow {
    Long getProductId();
    String getProductName();
    Long getQuantitySold();
    BigDecimal getRevenue();
}
