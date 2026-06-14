package com.pos.entity;

import com.pos.entity.enums.AdjustmentReason;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Một lần XUẤT HỦY / GIẢM TỒN cho một LÔ ({@link GoodsReceiptItem}) — bảng {@code stock_adjustments}.
 * Append-only (giống phiếu lên kệ / về kho): mỗi dòng làm GIẢM {@code quantity} đơn vị của lô khỏi KHO.
 * View {@code v_batch_stock} trừ tổng {@code quantity} này khỏi tồn kho & tồn còn lại của lô.
 * Quy ước: chỉ xuất hủy hàng đang TRONG KHO (hàng trên kệ phải "lấy về kho" trước) để giữ toàn vẹn tồn kệ.
 */
@Entity
@Table(name = "stock_adjustments")
@Getter
@Setter
@NoArgsConstructor
public class StockAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Chi nhánh phát sinh (đa chuỗi) — = chi nhánh của lô. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    /** Lô bị xuất hủy/giảm tồn. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private GoodsReceiptItem batch;

    /** Số lượng GIẢM (dương) — rút khỏi tồn kho của lô. */
    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdjustmentReason reason;

    @Column(length = 255)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
