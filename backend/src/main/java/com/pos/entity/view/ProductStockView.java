package com.pos.entity.view;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

/** Tồn kho hiện tại từng sản phẩm (FR8.1) — ánh xạ view {@code v_product_stock} (chỉ đọc). */
@Entity
@Immutable
@Table(name = "v_product_stock")
@Getter
public class ProductStockView {

    @Id
    @Column(name = "product_id")
    private Long productId;

    private String barcode;
    private String name;

    @Column(name = "min_stock")
    private Integer minStock;

    @Column(name = "current_stock")
    private Long currentStock;

    /** Tồn trên KỆ (bán được tại POS). */
    @Column(name = "shelf_stock")
    private Long shelfStock;

    /** Tồn trong KHO (chưa lên kệ). */
    @Column(name = "warehouse_stock")
    private Long warehouseStock;
}
