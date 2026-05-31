package com.pos.dto.returns;

import java.math.BigDecimal;

/** Một dòng của hóa đơn cùng số CÒN ĐƯỢC TRẢ — cho màn chọn trả hàng. */
public record ReturnableLineResponse(
        Long invoiceItemId,
        Long productId,
        String productName,
        Integer soldQty,
        Integer returnedQty,
        Integer returnableQty,
        BigDecimal unitPrice
) {}
