package com.pos.repository.projection;

import java.math.BigDecimal;

/** Doanh số bán theo từng sản phẩm trong khoảng — báo cáo lợi nhuận sản phẩm. */
public interface ProductSalesDetailRow {
    Long getProductId();
    String getProductName();
    String getCategoryName();
    Long getQtySold();
    BigDecimal getRevenue();
}
