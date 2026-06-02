package com.pos.repository.projection;

import java.math.BigDecimal;

/** Doanh thu + sản lượng theo sản phẩm trong kỳ — dùng cho phân tích ABC/XYZ. */
public interface ProductRevenueRow {
    Long getProductId();
    BigDecimal getRevenue();
    Long getQty();
}
