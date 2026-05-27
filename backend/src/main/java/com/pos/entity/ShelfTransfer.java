package com.pos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Một lần CHUYỂN HÀNG TỪ KHO LÊN KỆ cho một LÔ (goods_receipt_items) — bảng {@code shelf_transfers}.
 *  Tồn kệ của lô = tổng đã chuyển lên kệ − đã bán; tồn kho của lô = đã nhập − đã chuyển lên kệ. */
@Entity
@Table(name = "shelf_transfers")
@Getter
@Setter
@NoArgsConstructor
public class ShelfTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Lô được đưa lên kệ. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private GoodsReceiptItem batch;

    @Column(nullable = false)
    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
