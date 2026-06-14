package com.pos.repository.projection;

/** Tổng số lượng hàng đã bán theo một người dùng (thu ngân) — báo cáo hiệu suất nhân viên. */
public interface UserQtyRow {
    Long getUserId();
    Long getQty();
}
