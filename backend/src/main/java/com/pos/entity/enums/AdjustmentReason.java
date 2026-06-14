package com.pos.entity.enums;

/**
 * Lý do XUẤT HỦY / GIẢM TỒN một lô hàng (kiểm kê, hao hụt). Mọi giá trị đều làm GIẢM tồn kho.
 * <ul>
 *   <li>{@code EXPIRED}  — hết hạn sử dụng (lô bị chặn bán, cần rút khỏi tồn).</li>
 *   <li>{@code DAMAGED}  — hư hỏng/vỡ/bể, không bán được.</li>
 *   <li>{@code LOST}     — thất thoát/mất mát/thiếu hụt khi kiểm kê.</li>
 *   <li>{@code OTHER}    — lý do khác (ghi rõ ở ghi chú).</li>
 *   <li>{@code TRANSFER_OUT} — xuất điều chuyển sang chi nhánh khác (Obj 1.1). Tồn rời kho NGUỒN;
 *       hàng được tái tạo ở chi nhánh ĐÍCH bằng phiếu nhập nội bộ khi nhận.</li>
 * </ul>
 */
public enum AdjustmentReason {
    EXPIRED,
    DAMAGED,
    LOST,
    OTHER,
    TRANSFER_OUT
}
