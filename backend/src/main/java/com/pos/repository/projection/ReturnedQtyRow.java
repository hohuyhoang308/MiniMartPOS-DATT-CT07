package com.pos.repository.projection;

/** Số lượng đã trả gộp theo dòng bán. */
public interface ReturnedQtyRow {
    Long getInvoiceItemId();
    Long getQty();
}
