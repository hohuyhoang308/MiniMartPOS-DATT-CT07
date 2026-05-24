package com.pos.repository.view;

import com.pos.entity.view.ProductStockView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductStockViewRepository extends JpaRepository<ProductStockView, Long> {

    Optional<ProductStockView> findByProductId(Long productId);

    /** Sản phẩm tồn thấp (FR8.2): current_stock <= min_stock. */
    @Query("SELECT v FROM ProductStockView v WHERE v.currentStock <= v.minStock ORDER BY v.currentStock")
    List<ProductStockView> findLowStock();

    /** Số sản phẩm đã hết hàng (tồn = 0). */
    @Query("SELECT COUNT(v) FROM ProductStockView v WHERE v.currentStock <= 0")
    long countOutOfStock();
}
