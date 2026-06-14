package com.pos.dto.transfer;

import com.pos.entity.StockTransfer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Thông tin phiếu điều chuyển (Obj 1.1). */
public record StockTransferResponse(
        Long id,
        String code,
        Long sourceStoreId,
        String sourceStoreName,
        Long destStoreId,
        String destStoreName,
        String status,
        String note,
        LocalDateTime createdAt,
        LocalDateTime shippedAt,
        LocalDateTime receivedAt,
        List<Item> items
) {
    public record Item(Long batchId, Long productId, String productName,
                       Integer quantity, LocalDate expiryDate, BigDecimal costPrice) {}

    public static StockTransferResponse from(StockTransfer t) {
        List<Item> items = t.getItems().stream()
                .map(it -> new Item(it.getBatch().getId(), it.getProduct().getId(),
                        it.getProduct().getName(), it.getQuantity(), it.getExpiryDate(), it.getCostPrice()))
                .toList();
        return new StockTransferResponse(
                t.getId(), t.getCode(),
                t.getSourceStore().getId(), t.getSourceStore().getName(),
                t.getDestStore().getId(), t.getDestStore().getName(),
                t.getStatus().name(), t.getNote(), t.getCreatedAt(),
                t.getShippedAt(), t.getReceivedAt(), items);
    }
}
