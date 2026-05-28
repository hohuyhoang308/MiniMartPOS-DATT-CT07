package com.pos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Một lần LẤY HÀNG TỪ KỆ VỀ KHO cho một LÔ (goods_receipt_items) — bảng {@code shelf_returns}.
 *  Đối ứng với {@link ShelfTransfer}: tồn kệ của lô = đã lên kệ − đã trả về kho − đã bán. */
@Entity
@Table(name = "shelf_returns")
@Getter
@Setter
@NoArgsConstructor
public class ShelfReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Lô được lấy từ kệ về kho. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private GoodsReceiptItem batch;

    /** Kệ nguồn (lấy hàng xuống từ kệ này). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shelf_id", nullable = false)
    private Shelf shelf;

    @Column(nullable = false)
    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
