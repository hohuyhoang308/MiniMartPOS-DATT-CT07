package com.pos.dto.inventory;

import com.pos.entity.StockAdjustment;
import com.pos.entity.enums.AdjustmentReason;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Một dòng lịch sử xuất hủy/giảm tồn — hiển thị cho MANAGER. */
public record StockAdjustmentResponse(
        Long id,
        Long batchId,
        Long productId,
        String productName,
        LocalDate expiryDate,
        Integer quantity,
        AdjustmentReason reason,
        String note,
        String createdByName,
        LocalDateTime createdAt
) {
    public static StockAdjustmentResponse from(StockAdjustment a) {
        var batch = a.getBatch();
        var product = batch.getProduct();
        return new StockAdjustmentResponse(
                a.getId(),
                batch.getId(),
                product.getId(),
                product.getName(),
                batch.getExpiryDate(),
                a.getQuantity(),
                a.getReason(),
                a.getNote(),
                a.getCreatedBy() != null ? a.getCreatedBy().getFullName() : null,
                a.getCreatedAt());
    }
}
