package com.pos.repository.view;

import com.pos.entity.view.BatchStockView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BatchStockViewRepository extends JpaRepository<BatchStockView, Long> {

    /** Các lô còn tồn TRÊN KỆ của 1 sản phẩm, FIFO theo HSD — dùng để bán (POS chỉ bán hàng trên kệ). */
    @Query("""
            SELECT v FROM BatchStockView v
            WHERE v.productId = :productId AND v.onShelf > 0
            ORDER BY CASE WHEN v.expiryDate IS NULL THEN 1 ELSE 0 END, v.expiryDate, v.batchId
            """)
    List<BatchStockView> findAvailableBatchesFifo(@Param("productId") Long productId);

    /** Các lô còn tồn TRONG KHO của 1 sản phẩm, FIFO theo HSD — dùng để chọn lô đưa lên kệ. */
    @Query("""
            SELECT v FROM BatchStockView v
            WHERE v.productId = :productId AND v.inWarehouse > 0
            ORDER BY CASE WHEN v.expiryDate IS NULL THEN 1 ELSE 0 END, v.expiryDate, v.batchId
            """)
    List<BatchStockView> findWarehouseBatchesFifo(@Param("productId") Long productId);

    /** Tất cả lô CÒN HÀNG của 1 sản phẩm (FIFO theo HSD) — xem chi tiết kho/kệ theo lô. */
    @Query("""
            SELECT v FROM BatchStockView v
            WHERE v.productId = :productId AND v.quantityRemaining > 0
            ORDER BY CASE WHEN v.expiryDate IS NULL THEN 1 ELSE 0 END, v.expiryDate, v.batchId
            """)
    List<BatchStockView> findProductBatches(@Param("productId") Long productId);
}
