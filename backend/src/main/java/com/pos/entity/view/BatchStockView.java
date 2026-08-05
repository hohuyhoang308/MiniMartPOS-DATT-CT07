package com.pos.entity.view;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;

/** Tồn còn lại từng LÔ = số nhập - tổng đã bán (HĐ COMPLETED) —
 *  ánh xạ view {@code v_batch_stock} (chỉ đọc). Dùng để chọn lô FEFO khi bán. */
@Entity
@Immutable
@Table(name = "v_batch_stock")
@Getter
public class BatchStockView {

    @Id
    @Column(name = "batch_id")
    private Long batchId;

    @Column(name = "product_id")
    private Long productId;

    /** Chi nhánh của lô (thừa hưởng từ phiếu nhập) — để lọc tồn theo chi nhánh. */
    @Column(name = "store_id")
    private Long storeId;

    /** Kệ mà lô này đang nằm (NULL nếu chưa lên kệ). */
    @Column(name = "shelf_id")
    private Long shelfId;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "quantity_in")
    private Integer quantityIn;

    @Column(name = "quantity_remaining")
    private Long quantityRemaining;

    /** Tồn trên KỆ của lô (đã chuyển lên kệ − đã bán) — POS bán từ đây. */
    @Column(name = "on_shelf")
    private Long onShelf;

    /** Tồn trong KHO của lô (đã nhập − đã chuyển lên kệ). */
    @Column(name = "in_warehouse")
    private Long inWarehouse;
}
