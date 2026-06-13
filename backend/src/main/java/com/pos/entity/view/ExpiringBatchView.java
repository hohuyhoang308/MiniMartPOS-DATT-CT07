package com.pos.entity.view;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;

/** Lô còn hàng & cận/quá HSD trong 30 ngày tới (FR8.2) —
 *  ánh xạ view {@code v_expiring_batches} (chỉ đọc). */
@Entity
@Immutable
@Table(name = "v_expiring_batches")
@Getter
public class ExpiringBatchView {

    @Id
    @Column(name = "batch_id")
    private Long batchId;

    @Column(name = "product_id")
    private Long productId;

    /** Chi nhánh của lô (đa chuỗi). */
    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "quantity_remaining")
    private Long quantityRemaining;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "days_left")
    private Integer daysLeft;
}
