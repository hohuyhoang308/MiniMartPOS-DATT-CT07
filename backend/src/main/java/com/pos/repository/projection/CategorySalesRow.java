package com.pos.repository.projection;

import java.math.BigDecimal;

/** Doanh thu & sản lượng bán theo danh mục. */
public interface CategorySalesRow {
    String getCategoryName();
    BigDecimal getRevenue();
    Long getQuantity();
}
