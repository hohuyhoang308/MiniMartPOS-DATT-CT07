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
        Long warehouseStock,  // tồn trong KHO (chưa lên kệ)
        String shelfCode,     // mã kệ đang bày (vd K06) — null nếu chưa lên kệ
        BigDecimal storeSalePrice // giá bán HIỆU LỰC tại chi nhánh đang xem (override); null = dùng salePrice gốc.
                                  // POS phải tính/hiển thị theo (storeSalePrice ?? salePrice) để KHỚP với giá server tính tiền.
) {
    public static ProductResponse from(Product p, Long currentStock, Long shelfStock, Long warehouseStock,
                                        String shelfCode) {
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
                nz(currentStock), nz(shelfStock), nz(warehouseStock), shelfCode, null);
    }

    /** Bản sao đính GIÁ HIỆU LỰC theo chi nhánh (override). {@code salePrice} gốc GIỮ NGUYÊN để trang
     *  danh mục (HQ) sửa đúng giá chuẩn; chỉ POS đọc {@code storeSalePrice}. */
    public ProductResponse withStoreSalePrice(BigDecimal effective) {
        return new ProductResponse(
                id, barcode, name, categoryId, categoryName, unitId, unitName,
                costPrice, salePrice, taxRate, packSize, packUnitId, packUnitName, imageUrl,
                minStock, status, currentStock, shelfStock, warehouseStock, shelfCode, effective);
    }

    public static ProductResponse from(Product p, Long currentStock, Long shelfStock, Long warehouseStock) {
        return from(p, currentStock, shelfStock, warehouseStock, null);
    }

    /** Tiện dụng từ view tồn kho (gồm tách kho/kệ). */
    public static ProductResponse from(Product p, ProductStockView v) {
        return from(p, v, null);
    }

    /** Như trên, kèm MÃ KỆ đang bày (cho POS/danh sách biết hàng ở kệ nào). */
    public static ProductResponse from(Product p, ProductStockView v, String shelfCode) {
        return v == null ? from(p, 0L, 0L, 0L, shelfCode)
                : from(p, v.getCurrentStock(), v.getShelfStock(), v.getWarehouseStock(), shelfCode);
    }

    private static Long nz(Long v) {
        return v != null ? v : 0L;
    }
}
