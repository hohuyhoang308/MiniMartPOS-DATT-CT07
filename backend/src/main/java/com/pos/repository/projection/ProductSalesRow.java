package com.pos.repository.projection;

/** Sản lượng đã bán của một sản phẩm trong khoảng (phục vụ tính tốc độ bán). */
public interface ProductSalesRow {
    Long getProductId();
    Long getSoldQty();
}
