package com.pos.entity.view;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

/** Tổng chi tiêu + số HĐ của khách (FR6.2) — ánh xạ view {@code v_customer_spending} (chỉ đọc). */
@Entity
@Immutable
@Table(name = "v_customer_spending")
@Getter
public class CustomerSpendingView {

    @Id
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "full_name")
    private String fullName;

    private String phone;

    @Column(name = "loyalty_points")
    private Integer loyaltyPoints;

    @Column(name = "total_spent")
    private BigDecimal totalSpent;

    @Column(name = "invoice_count")
    private Long invoiceCount;
}
