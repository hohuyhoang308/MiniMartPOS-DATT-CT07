package com.pos.entity.enums;

/** Trạng thái giao dịch thanh toán QR/WEB2M (FR-A4). */
public enum PaymentStatus {
    PENDING,   // chờ đối soát
    PAID,      // đã khớp giao dịch ngân hàng
    EXPIRED,   // hết hạn QR
    FAILED
}
