package com.pos.entity;

import com.pos.entity.enums.InvoiceStatus;
import com.pos.entity.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Hóa đơn (FR4, FR5) — bảng {@code invoices}.
 *  KHÔNG có cashier_id: thu ngân suy ra qua shift → work_shifts.user.
 *  {@code total_amount} là cột GENERATED STORED = subtotal - discount_amount. */
@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    /** Ca làm việc ⇒ suy ra thu ngân. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private WorkShift shift;

    /** Khách thân thiết (tùy chọn). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    /** Mã giảm giá (tùy chọn). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /** Cột GENERATED STORED do CSDL tự tính = subtotal - discount_amount (chỉ đọc). */
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "total_amount", insertable = false, updatable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    /** Tiền khách đưa (tiền mặt). */
    @Column(name = "customer_paid", precision = 14, scale = 2)
    private BigDecimal customerPaid;

    /** Tiền thừa. */
    @Column(name = "change_amount", precision = 14, scale = 2)
    private BigDecimal changeAmount;

    @Column(name = "points_earned", nullable = false)
    private Integer pointsEarned = 0;

    /** Số điểm khách đã DÙNG để giảm trừ ở hóa đơn này (đối ứng với phần giảm trong discount_amount). */
    @Column(name = "points_used", nullable = false)
    private Integer pointsUsed = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status = InvoiceStatus.COMPLETED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceItem> items = new ArrayList<>();

    public void addItem(InvoiceItem item) {
        item.setInvoice(this);
        items.add(item);
    }
}
