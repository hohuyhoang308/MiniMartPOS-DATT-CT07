package com.pos.entity.enums;

/** Nguồn gốc phiếu nhập kho (Obj 1.1): mua từ NCC hay nhận điều chuyển nội bộ. */
public enum ReceiptSource {
    PURCHASE,   // nhập mua từ nhà cung cấp
    TRANSFER    // nhập do nhận điều chuyển từ chi nhánh khác
}
