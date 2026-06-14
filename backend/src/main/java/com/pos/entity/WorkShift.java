package com.pos.entity;

import com.pos.entity.enums.ShiftStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Ca làm việc (FR4.1) — bảng {@code work_shifts}.
 *  Doanh thu ca KHÔNG lưu — suy ra qua view v_shift_summary. */
@Entity
@Table(name = "work_shifts")
@Getter
@Setter
@NoArgsConstructor
public class WorkShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Chi nhánh mở ca (đa chuỗi) — hóa đơn/phiếu trả của ca thuộc về chi nhánh đây. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    /** Thu ngân của ca. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "opening_cash", nullable = false, precision = 12, scale = 2)
    private BigDecimal openingCash = BigDecimal.ZERO;

    /** Tiền đối soát cuối ca (NULL khi đang mở). */
    @Column(name = "closing_cash", precision = 12, scale = 2)
    private BigDecimal closingCash;

    /**
     * SNAPSHOT tiền-mặt-bán CHỐT lúc đóng ca (finding #3) — NULL khi ca đang mở.
     * Đối soát quỹ của ca ĐÃ ĐÓNG dùng giá trị này thay vì tính lại từ hóa đơn, để việc hủy HĐ tiền mặt
     * về sau (ở ca khác) không làm thay đổi chênh lệch quỹ đã chốt của ca này.
     */
    @Column(name = "final_cash_sales", precision = 14, scale = 2)
    private BigDecimal finalCashSales;

    @CreationTimestamp
    @Column(name = "opened_at", nullable = false, updatable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShiftStatus status = ShiftStatus.OPEN;
}
