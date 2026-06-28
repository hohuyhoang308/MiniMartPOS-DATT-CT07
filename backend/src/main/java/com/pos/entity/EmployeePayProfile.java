package com.pos.entity;

import com.pos.entity.enums.PayType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Cấu hình lương của một nhân viên (module Lương) — bảng {@code employee_pay_profiles}.
 *  1 dòng / nhân viên (cấu hình hiện hành); đổi mức lương được ghi audit_logs. */
@Entity
@Table(name = "employee_pay_profiles")
@Getter
@Setter
@NoArgsConstructor
public class EmployeePayProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "pay_type", nullable = false)
    private PayType payType = PayType.MONTHLY;

    /** HOURLY: đồng/giờ · MONTHLY: đồng/tháng. */
    @Column(name = "base_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseRate = BigDecimal.ZERO;

    /** Công chuẩn/tháng (26 ngày × 8h = 208) — mốc tính tăng ca & quy đổi đơn giá giờ cho lương tháng. */
    @Column(name = "standard_monthly_hours", nullable = false, precision = 6, scale = 2)
    private BigDecimal standardMonthlyHours = new BigDecimal("208");

    @Column(name = "ot_multiplier", nullable = false, precision = 4, scale = 2)
    private BigDecimal otMultiplier = new BigDecimal("1.5");

    @Column(name = "monthly_allowance", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyAllowance = BigDecimal.ZERO;

    @Column(name = "updated_by")
    private Long updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
