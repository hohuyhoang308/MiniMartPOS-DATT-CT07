package com.pos.repository.view;

import com.pos.entity.view.BatchStockView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BatchStockViewRepository extends JpaRepository<BatchStockView, Long> {

    /** Các lô còn tồn của 1 sản phẩm, sắp xếp FIFO theo HSD (NULL HSD xuống cuối), rồi theo batch_id. */
    @Query("""
            SELECT v FROM BatchStockView v
            WHERE v.productId = :productId AND v.quantityRemaining > 0
            ORDER BY CASE WHEN v.expiryDate IS NULL THEN 1 ELSE 0 END, v.expiryDate, v.batchId
            """)
    List<BatchStockView> findAvailableBatchesFifo(@Param("productId") Long productId);
}
