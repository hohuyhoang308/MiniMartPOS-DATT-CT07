package com.pos.dto.returns;

import com.pos.entity.SalesReturn;
import com.pos.entity.SalesReturnItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Chứng từ trả hàng đã tạo. */
public record ReturnResponse(
        Long id,
        Long invoiceId,
        String invoiceCode,
        String reason,
        BigDecimal refundAmount,
        String createdBy,
        LocalDateTime createdAt,
        List<Item> items
) {
    public record Item(String productName, Integer quantity, BigDecimal unitPrice, BigDecimal lineRefund) {}

    public static ReturnResponse from(SalesReturn r) {
        List<Item> items = r.getItems().stream().map(ReturnResponse::lineOf).toList();
        return new ReturnResponse(
                r.getId(), r.getInvoice().getId(), r.getInvoice().getCode(), r.getReason(),
                r.getRefundAmount(),
                r.getCreatedBy() != null ? r.getCreatedBy().getFullName() : null,
                r.getCreatedAt(), items);
    }

    private static Item lineOf(SalesReturnItem ri) {
        BigDecimal line = ri.getUnitPrice().multiply(BigDecimal.valueOf(ri.getQuantity()));
        String name = ri.getInvoiceItem().getProduct() != null ? ri.getInvoiceItem().getProduct().getName() : "?";
        return new Item(name, ri.getQuantity(), ri.getUnitPrice(), line);
    }
}
