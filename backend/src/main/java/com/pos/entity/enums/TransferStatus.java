package com.pos.entity.enums;

/**
 * Trạng thái phiếu ĐIỀU CHUYỂN HÀNG NỘI BỘ (Obj 1.1). Chuyển trạng thái MỘT CHIỀU, chặt:
 * <pre>
 *   PENDING ──ship──▶ SHIPPING ──receive──▶ RECEIVED   (kết thúc)
 *      └────────cancel────────┘ (chỉ huỷ khi chưa RECEIVED)
 * </pre>
 * Mọi bước đều có bảng chuyển hợp lệ ở {@code StockTransferService} (defensive — chặn nhảy trạng thái).
 */
public enum TransferStatus {
    PENDING,
    SHIPPING,
    RECEIVED,
    CANCELLED
}
