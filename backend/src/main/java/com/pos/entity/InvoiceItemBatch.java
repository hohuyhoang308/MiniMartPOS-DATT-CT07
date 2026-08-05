package com.pos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Phân bổ tồn theo lô khi bán (bảng nối) — bảng {@code invoice_item_batches}.
 *  Nối dòng bán ↔ lô tồn kho (FEFO). Hủy HĐ ⇒ tồn tự hoàn vì các phân bổ
 *  trỏ tới hóa đơn CANCELLED không còn được tính (view v_batch_stock). */
@Entity
@Table(name = "invoice_item_batches")
@Getter
@Setter
@NoArgsConstructor
public class InvoiceItemBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_item_id", nullable = false)
    private InvoiceItem invoiceItem;

    /** Lô lấy hàng = goods_receipt_items.id. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private GoodsReceiptItem batch;

    @Column(nullable = false)
    private Integer quantity;
}
