package com.pos.entity.view;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;

/** Tồn còn lại từng LÔ = số nhập - tổng đã bán (HĐ COMPLETED) —
 *  ánh xạ view {@code v_batch_stock} (chỉ đọc). Dùng để chọn lô FIFO khi bán. */
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

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "quantity_in")
    private Integer quantityIn;

    @Column(name = "quantity_remaining")
    private Long quantityRemaining;
}
