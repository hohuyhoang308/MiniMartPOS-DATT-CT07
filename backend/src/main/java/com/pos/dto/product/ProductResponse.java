package com.pos.dto.product;

import com.pos.entity.Product;
import com.pos.entity.enums.CommonStatus;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String barcode,
        String name,
        Long categoryId,
        String categoryName,
        Long unitId,
        String unitName,
        BigDecimal costPrice,
        BigDecimal salePrice,
        String imageUrl,
        Integer minStock,
        CommonStatus status,
        Long currentStock
) {
    public static ProductResponse from(Product p, Long currentStock) {
        return new ProductResponse(
                p.getId(), p.getBarcode(), p.getName(),
                p.getCategory().getId(), p.getCategory().getName(),
                p.getUnit().getId(), p.getUnit().getName(),
                p.getCostPrice(), p.getSalePrice(), p.getImageUrl(),
                p.getMinStock(), p.getStatus(),
                currentStock != null ? currentStock : 0L);
    }
}
