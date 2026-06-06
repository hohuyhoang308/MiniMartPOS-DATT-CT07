package com.pos.dto.inventory;

import com.pos.entity.view.ProductStockView;

/**
 * Tồn kho 1 sản phẩm, tách KHO/KỆ.
 * @param shelfLow tồn kệ thấp (≤ ngưỡng) nhưng KHO còn hàng → cần "lên kệ".
 */
public record StockResponse(Long productId, String barcode, String name,
                            Integer minStock, Long currentStock, Long shelfStock, Long warehouseStock,
                            boolean lowStock, boolean shelfLow, String shelfCode) {

    public static StockResponse from(ProductStockView v, String shelfCode) {
        long total = nz(v.getCurrentStock()), shelf = nz(v.getShelfStock()), wh = nz(v.getWarehouseStock());
        int min = v.getMinStock() != null ? v.getMinStock() : 0;
        boolean low = total <= min;
        boolean shelfLow = shelf <= min && wh > 0;
        return new StockResponse(v.getProductId(), v.getBarcode(), v.getName(),
                v.getMinStock(), total, shelf, wh, low, shelfLow, shelfCode);
    }

    private static long nz(Long v) {
        return v != null ? v : 0L;
    }
}
