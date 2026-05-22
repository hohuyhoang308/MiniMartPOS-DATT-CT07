package com.pos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Dòng phiếu nhập = LÔ HÀNG (bất biến) — bảng {@code goods_receipt_items}.
 *  id cũng chính là batch_id. Tồn còn lại KHÔNG lưu ở đây — suy ra qua
 *  invoice_item_batches (view v_batch_stock). */
@Entity
@Table(name = "goods_receipt_items")
@Getter
@Setter
@NoArgsConstructor
public class GoodsReceiptItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receipt_id", nullable = false)
    private GoodsReceipt receipt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Số lượng nhập của lô (cố định). */
    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "import_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal importPrice;

    /** Hạn sử dụng của lô (NULL nếu không có HSD). */
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
}
