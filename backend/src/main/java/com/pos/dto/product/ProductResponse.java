package com.pos.dto.product;

import com.pos.entity.Product;
import com.pos.entity.enums.CommonStatus;
import com.pos.entity.view.ProductStockView;

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
        BigDecimal taxRate,
        Integer packSize,
        Long packUnitId,
        String packUnitName,
        String imageUrl,
        Integer minStock,
        CommonStatus status,
        Long currentStock,    // tổng tồn (kho + kệ)
        Long shelfStock,      // tồn trên KỆ (bán được tại POS)
        Long warehouseStock   // tồn trong KHO (chưa lên kệ)
) {
    public static ProductResponse from(Product p, Long currentStock, Long shelfStock, Long warehouseStock) {
        return new ProductResponse(
                p.getId(), p.getBarcode(), p.getName(),
                p.getCategory().getId(), p.getCategory().getName(),
                p.getUnit().getId(), p.getUnit().getName(),
                p.getCostPrice(), p.getSalePrice(), p.getTaxRate(),
                p.getPackSize(),
                p.getPackUnit() != null ? p.getPackUnit().getId() : null,
                p.getPackUnit() != null ? p.getPackUnit().getName() : null,
                p.getImageUrl(),
                p.getMinStock(), p.getStatus(),
                nz(currentStock), nz(shelfStock), nz(warehouseStock));
    }

    /** Tiện dụng từ view tồn kho (gồm tách kho/kệ). */
    public static ProductResponse from(Product p, ProductStockView v) {
        return v == null ? from(p, 0L, 0L, 0L)
                : from(p, v.getCurrentStock(), v.getShelfStock(), v.getWarehouseStock());
    }

    private static Long nz(Long v) {
        return v != null ? v : 0L;
    }
}
