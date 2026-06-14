package com.pos.entity.enums;

/**
 * Lý do XUẤT HỦY / GIẢM TỒN một lô hàng (kiểm kê, hao hụt). Mọi giá trị đều làm GIẢM tồn kho.
 * <ul>
 *   <li>{@code EXPIRED}  — hết hạn sử dụng (lô bị chặn bán, cần rút khỏi tồn).</li>
 *   <li>{@code DAMAGED}  — hư hỏng/vỡ/bể, không bán được.</li>
 *   <li>{@code LOST}     — thất thoát/mất mát/thiếu hụt khi kiểm kê.</li>
 *   <li>{@code OTHER}    — lý do khác (ghi rõ ở ghi chú).</li>
 * </ul>
 */
public enum AdjustmentReason {
    EXPIRED,
    DAMAGED,
    LOST,
    OTHER
}
