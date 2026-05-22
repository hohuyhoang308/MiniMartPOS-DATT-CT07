package com.pos.entity.enums;

/** Hình thức thanh toán của hóa đơn (FR4.6). */
public enum PaymentMethod {
    CASH,   // tiền mặt — thu tại quầy
    QR      // QR/chuyển khoản — đối soát qua WEB2M
}
