package com.pos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Khách hàng thân thiết (FR6) — bảng {@code customers}.
 *  Không lưu {@code total_spent} (suy ra qua view v_customer_spending). */
@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    /** SĐT duy nhất — khóa tra cứu khách. */
    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    /** Số dư điểm tích lũy (trạng thái). */
    @Column(name = "loyalty_points", nullable = false)
    private Integer loyaltyPoints = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
