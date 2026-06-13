package com.pos.repository.view;

import com.pos.entity.view.BatchStockView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BatchStockViewRepository extends JpaRepository<BatchStockView, Long> {

    /**
     * Các lô còn tồn TRÊN KỆ của 1 sản phẩm, FIFO theo HSD — dùng để bán (POS chỉ bán hàng trên kệ).
     * KHÔNG lấy lô đã QUÁ HẠN SỬ DỤNG (expiry < hôm nay): không được bán hàng hết hạn; nhân viên phải
     * lấy về kho & ghi nhận huỷ. Lô không có HSD vẫn bán bình thường.
     */
    @Query("""
            SELECT v FROM BatchStockView v
            WHERE v.productId = :productId AND v.storeId = :storeId AND v.onShelf > 0
              AND (v.expiryDate IS NULL OR v.expiryDate >= CURRENT_DATE)
            ORDER BY CASE WHEN v.expiryDate IS NULL THEN 1 ELSE 0 END, v.expiryDate, v.batchId
            """)
    List<BatchStockView> findAvailableBatchesFifo(@Param("productId") Long productId,
                                                  @Param("storeId") Long storeId);

    /** Các lô còn tồn TRONG KHO của 1 sản phẩm tại 1 CHI NHÁNH, FIFO theo HSD — dùng để chọn lô đưa lên kệ. */
    @Query("""
            SELECT v FROM BatchStockView v
            WHERE v.productId = :productId AND v.storeId = :storeId AND v.inWarehouse > 0
            ORDER BY CASE WHEN v.expiryDate IS NULL THEN 1 ELSE 0 END, v.expiryDate, v.batchId
            """)
    List<BatchStockView> findWarehouseBatchesFifo(@Param("productId") Long productId,
                                                  @Param("storeId") Long storeId);

    /** Tất cả lô CÒN HÀNG của 1 sản phẩm tại 1 chi nhánh (FIFO theo HSD) — xem chi tiết kho/kệ theo lô. */
    @Query("""
            SELECT v FROM BatchStockView v
            WHERE v.productId = :productId AND v.storeId = :storeId AND v.quantityRemaining > 0
            ORDER BY CASE WHEN v.expiryDate IS NULL THEN 1 ELSE 0 END, v.expiryDate, v.batchId
            """)
    List<BatchStockView> findProductBatches(@Param("productId") Long productId,
                                            @Param("storeId") Long storeId);

    /** Lô còn hàng trên kệ của 1 chi nhánh (gộp mọi sản phẩm) — thống kê kệ theo chi nhánh. */
    @Query("""
            SELECT v FROM BatchStockView v
            WHERE v.storeId = :storeId AND v.onShelf > 0
            """)
    List<BatchStockView> findOnShelfByStore(@Param("storeId") Long storeId);

    /** Các lô đang nằm TRÊN một KỆ (còn tồn kệ) — xem kệ chứa gì, lô/HSD nào. */
    @Query("""
            SELECT v FROM BatchStockView v
            WHERE v.shelfId = :shelfId AND v.onShelf > 0
            ORDER BY CASE WHEN v.expiryDate IS NULL THEN 1 ELSE 0 END, v.expiryDate, v.batchId
            """)
    List<BatchStockView> findByShelf(@Param("shelfId") Long shelfId);

    /**
     * ĐỐI SOÁT TOÀN VẸN: các lô có tồn ÂM (kệ hoặc kho). Bất biến đúng thì danh sách này LUÔN rỗng —
     * có dòng nghĩa là một mutation tồn đã lọt qua khóa/bị ghi sai. Dùng cho job cảnh báo định kỳ.
     */
    @Query("SELECT v FROM BatchStockView v WHERE v.onShelf < 0 OR v.inWarehouse < 0")
    List<BatchStockView> findNegativeStock();
}
