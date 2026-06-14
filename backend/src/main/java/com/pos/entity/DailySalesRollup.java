package com.pos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * TỔNG HỢP DOANH THU NGÀY (Obj 2 — rollup) — 1 dòng / (chi nhánh, ngày). Bảng {@code daily_sales_rollup}.
 *
 * <p>Bảng này là DỮ LIỆU SUY RA (job nền tổng hợp từ {@code invoices}), KHÔNG phải nguồn sự thật.
 * Có thể dựng lại bất cứ lúc nào từ hóa đơn. Báo cáo nhiều năm × nhiều chi nhánh đọc bảng nhỏ này
 * thay vì quét hàng triệu dòng hóa đơn thô → giữ hiệu năng khi dữ liệu lớn dần.</p>
 */
@Entity
@Table(name = "daily_sales_rollup")
@Getter
@Setter
@NoArgsConstructor
public class DailySalesRollup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "sales_date", nullable = false)
    private LocalDate salesDate;

    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal revenue = BigDecimal.ZERO;

    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal tax = BigDecimal.ZERO;

    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal cogs = BigDecimal.ZERO;

    @Column(name = "gross_profit", nullable = false, precision = 16, scale = 2)
    private BigDecimal grossProfit = BigDecimal.ZERO;

    @Column(name = "invoice_count", nullable = false)
    private Integer invoiceCount = 0;

    @Column(name = "items_sold", nullable = false)
    private Integer itemsSold = 0;

    @Column(name = "rolled_at", nullable = false)
    private LocalDateTime rolledAt;
}
