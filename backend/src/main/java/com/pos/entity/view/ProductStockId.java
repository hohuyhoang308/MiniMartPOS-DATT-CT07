package com.pos.entity.view;

import java.io.Serializable;
import java.util.Objects;

/** Khóa kép của {@link ProductStockView} sau khi tồn kho tách theo CHI NHÁNH (đa chuỗi). */
public class ProductStockId implements Serializable {

    private Long productId;
    private Long storeId;

    public ProductStockId() {}

    public ProductStockId(Long productId, Long storeId) {
        this.productId = productId;
        this.storeId = storeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductStockId that)) return false;
        return Objects.equals(productId, that.productId) && Objects.equals(storeId, that.storeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, storeId);
    }
}
