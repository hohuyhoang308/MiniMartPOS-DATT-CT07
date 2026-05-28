package com.pos.dto.shelf;

import com.pos.entity.view.BatchStockView;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Một LÔ đang nằm trên kệ: sản phẩm gì, HSD, số lượng — để biết "kệ chứa gì, lô nào". */
public record ShelfItemResponse(
        Long productId, String productName,
        Long batchId, LocalDate expiryDate, Integer daysLeft, Long quantity) {

    public static ShelfItemResponse from(BatchStockView v, String productName) {
        Integer days = v.getExpiryDate() != null
                ? (int) ChronoUnit.DAYS.between(LocalDate.now(), v.getExpiryDate()) : null;
        return new ShelfItemResponse(v.getProductId(), productName, v.getBatchId(),
                v.getExpiryDate(), days, v.getOnShelf());
    }
}
