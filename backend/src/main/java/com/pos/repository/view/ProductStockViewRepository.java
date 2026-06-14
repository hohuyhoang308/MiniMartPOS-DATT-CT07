package com.pos.repository.view;

import com.pos.entity.view.ProductStockView;
import com.pos.entity.view.ProductStockId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductStockViewRepository extends JpaRepository<ProductStockView, ProductStockId> {

    /** Tồn của 1 sản phẩm tại 1 CHI NHÁNH (đa chuỗi). */
    Optional<ProductStockView> findByProductIdAndStoreId(Long productId, Long storeId);

    /** Tồn toàn bộ sản phẩm của 1 chi nhánh (màn tồn kho theo chi nhánh). */
    List<ProductStockView> findByStoreId(Long storeId);

    /** Sản phẩm tồn thấp (FR8.2) của 1 chi nhánh: current_stock <= min_stock. */
    @Query("SELECT v FROM ProductStockView v WHERE v.storeId = :storeId AND v.currentStock <= v.minStock ORDER BY v.currentStock")
    List<ProductStockView> findLowStock(@Param("storeId") Long storeId);

    /** Số sản phẩm đã hết hàng (tồn = 0) của 1 chi nhánh. */
    @Query("SELECT COUNT(v) FROM ProductStockView v WHERE v.storeId = :storeId AND v.currentStock <= 0")
    long countOutOfStock(@Param("storeId") Long storeId);

    /** Tồn thấp TOÀN CHUỖI (mỗi dòng = 1 sản phẩm tại 1 chi nhánh) — dashboard của CHAIN_ADMIN. */
    @Query("SELECT v FROM ProductStockView v WHERE v.currentStock <= v.minStock ORDER BY v.currentStock")
    List<ProductStockView> findLowStockAll();

    /** Số (sản phẩm × chi nhánh) hết hàng toàn chuỗi. */
    @Query("SELECT COUNT(v) FROM ProductStockView v WHERE v.currentStock <= 0")
    long countOutOfStockAll();

    /** Số mặt hàng TỒN THẤP gộp theo từng chi nhánh — màn so sánh chi nhánh (1 truy vấn cho mọi cửa hàng). */
    @Query("SELECT v.storeId AS storeId, COUNT(v) AS count FROM ProductStockView v WHERE v.currentStock <= v.minStock GROUP BY v.storeId")
    List<com.pos.repository.projection.StoreCountRow> lowStockCountByStore();

    /** Số mặt hàng HẾT HÀNG gộp theo từng chi nhánh — màn so sánh chi nhánh. */
    @Query("SELECT v.storeId AS storeId, COUNT(v) AS count FROM ProductStockView v WHERE v.currentStock <= 0 GROUP BY v.storeId")
    List<com.pos.repository.projection.StoreCountRow> outOfStockCountByStore();
}
