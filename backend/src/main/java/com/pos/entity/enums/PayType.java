package com.pos.entity.enums;

/** Loại lương của nhân viên (module Lương). */
public enum PayType {
    /** Lương theo GIỜ: {@code base_rate} = đồng/giờ. */
    HOURLY,
    /** Lương theo THÁNG: {@code base_rate} = đồng/tháng, trả theo công đã làm (đủ công = đủ lương). */
    MONTHLY
}
