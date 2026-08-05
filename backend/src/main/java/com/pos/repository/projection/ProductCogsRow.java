package com.pos.repository.projection;

import java.math.BigDecimal;

/** Giá vốn hàng bán (COGS) đích danh theo lô của từng sản phẩm trong khoảng — báo cáo lợi nhuận sản phẩm. */
public interface ProductCogsRow {
    Long getProductId();
    BigDecimal getCogs();
}
