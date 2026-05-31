package com.pos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Sổ cái điểm tích lũy — mỗi thay đổi điểm là một dòng (append-only) để truy vết & đối soát số dư. */
@Entity
@Table(name = "loyalty_point_ledger")
@Getter
@Setter
@NoArgsConstructor
public class LoyaltyPointLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(nullable = false)
    private Integer delta;

    @Column(nullable = false, length = 40)
    private String reason;

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
