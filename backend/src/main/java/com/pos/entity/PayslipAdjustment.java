package com.pos.entity;

import com.pos.entity.enums.PayslipAdjustmentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Dòng điều chỉnh trên phiếu lương: thưởng/phạt/tạm ứng — bảng {@code payslip_adjustments}. */
@Entity
@Table(name = "payslip_adjustments")
@Getter
@Setter
@NoArgsConstructor
public class PayslipAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payslip_id", nullable = false)
    private Payslip payslip;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayslipAdjustmentType type;

    /** Số tiền (luôn dương) — chiều cộng/trừ do {@link #type} quyết định. */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
