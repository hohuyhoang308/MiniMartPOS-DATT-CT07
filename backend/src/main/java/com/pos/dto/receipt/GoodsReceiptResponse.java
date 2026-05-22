package com.pos.dto.receipt;

import com.pos.entity.GoodsReceipt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record GoodsReceiptResponse(
        Long id,
        String code,
        Long supplierId,
        String supplierName,
        String createdByName,
        BigDecimal totalAmount,
        String note,
        LocalDateTime createdAt,
        List<Item> items
) {
    public record Item(Long id, Long productId, String productName,
                       Integer quantity, BigDecimal importPrice, LocalDate expiryDate) {}

    public static GoodsReceiptResponse from(GoodsReceipt r) {
        List<Item> items = r.getItems().stream()
                .map(it -> new Item(it.getId(), it.getProduct().getId(), it.getProduct().getName(),
                        it.getQuantity(), it.getImportPrice(), it.getExpiryDate()))
                .toList();
        return new GoodsReceiptResponse(
                r.getId(), r.getCode(),
                r.getSupplier().getId(), r.getSupplier().getName(),
                r.getCreatedBy().getFullName(),
                r.getTotalAmount(), r.getNote(), r.getCreatedAt(), items);
    }
}
