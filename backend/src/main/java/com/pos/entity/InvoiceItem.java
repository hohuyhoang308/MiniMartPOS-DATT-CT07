package com.pos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Chi tiết hóa đơn (FR4.3) — bảng {@code invoice_items}.
 *  {@code unit_price} là giá snapshot tại thời điểm bán (giữ giá lịch sử).
 *  {@code subtotal} là cột GENERATED STORED = quantity * unit_price. */
@Entity
@Table(name = "invoice_items")
@Getter
@Setter
@NoArgsConstructor
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    /** Giá bán tại thời điểm bán (snapshot — không lấy động từ products.sale_price). */
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    /** Cột GENERATED STORED do CSDL tự tính = quantity * unit_price (chỉ đọc). */
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(insertable = false, updatable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    /** Phân bổ tồn theo lô (FIFO) cho dòng bán này. */
    @OneToMany(mappedBy = "invoiceItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceItemBatch> batches = new ArrayList<>();

    public void addBatch(InvoiceItemBatch batch) {
        batch.setInvoiceItem(this);
        batches.add(batch);
    }
}
