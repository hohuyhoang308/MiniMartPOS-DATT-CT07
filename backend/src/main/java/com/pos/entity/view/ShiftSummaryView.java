package com.pos.entity.view;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Doanh thu + số HĐ theo ca/thu ngân (FR9.2) — ánh xạ view {@code v_shift_summary} (chỉ đọc). */
@Entity
@Immutable
@Table(name = "v_shift_summary")
@Getter
public class ShiftSummaryView {

    @Id
    @Column(name = "shift_id")
    private Long shiftId;

    /** Chi nhánh mở ca (đa chuỗi). */
    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "cashier_name")
    private String cashierName;

    @Column(name = "opening_cash")
    private BigDecimal openingCash;

    @Column(name = "closing_cash")
    private BigDecimal closingCash;

    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    private String status;

    @Column(name = "total_sales")
    private BigDecimal totalSales;

    @Column(name = "invoice_count")
    private Long invoiceCount;
}
