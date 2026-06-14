package com.pos.dto.pricing;

import com.pos.entity.ProductStorePrice;

import java.math.BigDecimal;

/** Giá riêng theo chi nhánh của một sản phẩm (Obj 1.3). */
public record StorePriceResponse(
        Long productId,
        String productName,
        Long storeId,
        BigDecimal storePrice,    // giá override của chi nhánh
        BigDecimal basePrice      // giá chuẩn của chuỗi (tham chiếu)
) {
    public static StorePriceResponse from(ProductStorePrice psp) {
        return new StorePriceResponse(
                psp.getProduct().getId(),
                psp.getProduct().getName(),
                psp.getStore().getId(),
                psp.getSalePrice(),
                psp.getProduct().getSalePrice());
    }
}
