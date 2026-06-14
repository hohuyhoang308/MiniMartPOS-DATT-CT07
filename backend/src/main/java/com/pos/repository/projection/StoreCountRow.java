package com.pos.repository.projection;

/** Số lượng (hóa đơn / mặt hàng tồn thấp…) gộp theo CHI NHÁNH — cho màn so sánh chi nhánh. */
public interface StoreCountRow {
    Long getStoreId();
    long getCount();
}
