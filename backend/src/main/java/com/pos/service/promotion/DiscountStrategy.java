package com.pos.service.promotion;

import com.pos.entity.Promotion;
import com.pos.entity.enums.DiscountType;

import java.math.BigDecimal;

/**
 * STRATEGY PATTERN (Obj 3) cho cách TÍNH SỐ TIỀN GIẢM của một khuyến mãi.
 *
 * <p>Lý do chọn Strategy: cách tính giảm là điểm BIẾN ĐỘNG (hiện có PERCENT/AMOUNT; tương lai có thể thêm
 * BOGO, giảm theo bậc, giảm theo nhóm hàng…). Đưa mỗi cách tính thành một lớp riêng giúp:
 * (1) bỏ khối if-else phình to trong service (OCP — thêm loại mới không sửa code cũ),
 * (2) test từng cách tính độc lập, (3) Spring tự gom các bean qua {@link DiscountStrategyFactory}.</p>
 */
public interface DiscountStrategy {

    /** Loại giảm giá mà strategy này phụ trách. */
    DiscountType type();

    /**
     * Tính số tiền giảm cho đơn có tạm tính {@code subtotal}. KHÔNG tự cap theo subtotal ở đây —
     * việc cap (không cho âm) do nơi gọi đảm nhận để giữ một chỗ duy nhất kiểm soát bất biến total ≥ 0.
     */
    BigDecimal computeDiscount(Promotion promotion, BigDecimal subtotal);
}
