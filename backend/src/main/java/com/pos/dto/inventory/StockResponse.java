package com.pos.dto.inventory;

import com.pos.entity.view.ProductStockView;

public record StockResponse(Long productId, String barcode, String name,
                            Integer minStock, Long currentStock, boolean lowStock) {

    public static StockResponse from(ProductStockView v) {
        boolean low = v.getCurrentStock() != null && v.getMinStock() != null
                && v.getCurrentStock() <= v.getMinStock();
        return new StockResponse(v.getProductId(), v.getBarcode(), v.getName(),
                v.getMinStock(), v.getCurrentStock(), low);
    }
}
