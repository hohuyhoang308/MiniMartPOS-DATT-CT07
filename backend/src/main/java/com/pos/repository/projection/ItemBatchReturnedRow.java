package com.pos.repository.projection;

/** Số lượng đã trả gộp theo (dòng bán, lô). */
public interface ItemBatchReturnedRow {
    Long getInvoiceItemId();
    Long getBatchId();
    Long getQty();
}
