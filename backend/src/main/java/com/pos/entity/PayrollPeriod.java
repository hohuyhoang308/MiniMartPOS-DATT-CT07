package com.pos.entity;

import com.pos.entity.enums.PayrollStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Kỳ lương của một chi nhánh trong một tháng (module Lương) — bảng {@code payroll_periods}.
 *  1 kỳ / (chi nhánh, tháng). Vòng đời DRAFT → LOCKED → PAID. */
@Entity
@Table(name = "payroll_periods")
@Getter
@Setter
@NoArgsConstructor
public class PayrollPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    /** Tháng lương dạng 'YYYY-MM'. */
    @Column(name = "period_month", nullable = false, length = 7)
    private String periodMonth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayrollStatus status = PayrollStatus.DRAFT;

    @Column(length = 255)
    private String note;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Người lập trình duyệt (DRAFT → PENDING_APPROVAL). */
    @Column(name = "submitted_by")
    private Long submittedBy;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    /** Người duyệt (PENDING_APPROVAL → APPROVED) — tách biệt trách nhiệm với người lập. */
    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;
}
