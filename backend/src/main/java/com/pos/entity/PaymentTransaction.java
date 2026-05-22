package com.pos.entity;

import com.pos.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Giao dịch thanh toán điện tử — VietQR + WEB2M (FR-A1, FR-A4) —
 *  bảng {@code payment_transactions}. Chỉ tạo cho thanh toán QR/chuyển khoản. */
@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    /** Số tiền cần khớp với giao dịch ngân hàng (snapshot). */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    /** Nội dung CK duy nhất để WEB2M map giao dịch → đúng HĐ (vd "POS HD...."). */
    @Column(name = "transfer_content", nullable = false, unique = true, length = 50)
    private String transferContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    /** Mã giao dịch NH do WEB2M trả về khi khớp (bằng chứng đã nhận tiền). */
    @Column(name = "bank_reference", length = 100)
    private String bankReference;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
