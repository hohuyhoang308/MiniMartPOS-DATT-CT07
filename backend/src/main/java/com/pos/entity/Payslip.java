package com.pos.entity;

import com.pos.entity.enums.PayType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Phiếu lương 1 nhân viên trong 1 kỳ (module Lương) — bảng {@code payslips}.
 *  SNAPSHOT toàn bộ số liệu (cấu hình + công + tiền) để kỳ đã khóa là cố định. */
@Entity
@Table(name = "payslips")
@Getter
@Setter
@NoArgsConstructor
public class Payslip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id", nullable = false)
    private PayrollPeriod period;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // --- snapshot cấu hình lương lúc tính ---
    @Enumerated(EnumType.STRING)
    @Column(name = "pay_type", nullable = false)
    private PayType payType;

    @Column(name = "base_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseRate = BigDecimal.ZERO;

    @Column(name = "standard_hours", nullable = false, precision = 8, scale = 2)
    private BigDecimal standardHours = BigDecimal.ZERO;

    // --- công (từ ca đã đóng) ---
    @Column(name = "worked_hours", nullable = false, precision = 8, scale = 2)
    private BigDecimal workedHours = BigDecimal.ZERO;

    @Column(name = "regular_hours", nullable = false, precision = 8, scale = 2)
    private BigDecimal regularHours = BigDecimal.ZERO;

    @Column(name = "ot_hours", nullable = false, precision = 8, scale = 2)
    private BigDecimal otHours = BigDecimal.ZERO;

    @Column(name = "shift_count", nullable = false)
    private Integer shiftCount = 0;

    // --- tiền ---
    @Column(name = "regular_pay", nullable = false, precision = 14, scale = 2)
    private BigDecimal regularPay = BigDecimal.ZERO;

    @Column(name = "ot_pay", nullable = false, precision = 14, scale = 2)
    private BigDecimal otPay = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal allowance = BigDecimal.ZERO;

    @Column(name = "gross_pay", nullable = false, precision = 14, scale = 2)
    private BigDecimal grossPay = BigDecimal.ZERO;

    @Column(name = "total_bonus", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalBonus = BigDecimal.ZERO;

    @Column(name = "total_deduction", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalDeduction = BigDecimal.ZERO;

    @Column(name = "net_pay", nullable = false, precision = 14, scale = 2)
    private BigDecimal netPay = BigDecimal.ZERO;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
