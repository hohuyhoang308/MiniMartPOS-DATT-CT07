package com.pos.dto.inventory;

import com.pos.entity.view.ExpiringBatchView;

import java.time.LocalDate;

public record ExpiringBatchResponse(Long batchId, Long productId, String productName,
                                    Long quantityRemaining, LocalDate expiryDate, Integer daysLeft) {

    public static ExpiringBatchResponse from(ExpiringBatchView v) {
        return new ExpiringBatchResponse(v.getBatchId(), v.getProductId(), v.getProductName(),
                v.getQuantityRemaining(), v.getExpiryDate(), v.getDaysLeft());
    }
}
