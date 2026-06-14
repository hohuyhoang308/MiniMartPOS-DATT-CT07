package com.pos.entity;

import com.pos.entity.enums.ReceiptSource;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Phiếu nhập kho (FR3.2) — bảng {@code goods_receipts}. */
@Entity
@Table(name = "goods_receipts")
@Getter
@Setter
@NoArgsConstructor
public class GoodsReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    /** Chi nhánh nhập hàng (đa chuỗi) — LÔ hàng của phiếu này thuộc về chi nhánh đây. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    /** NCC (NULL nếu phiếu do NHẬN ĐIỀU CHUYỂN nội bộ — Obj 1.1). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    /** Nguồn gốc: PURCHASE (mua NCC) hoặc TRANSFER (nhận điều chuyển). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReceiptSource source = ReceiptSource.PURCHASE;

    /** Người lập phiếu. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(length = 255)
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GoodsReceiptItem> items = new ArrayList<>();

    public void addItem(GoodsReceiptItem item) {
        item.setReceipt(this);
        items.add(item);
    }
}
