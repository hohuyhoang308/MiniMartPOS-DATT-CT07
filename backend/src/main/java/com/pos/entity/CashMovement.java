package com.pos.entity;

import com.pos.entity.enums.CashMovementType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Một lần THU/CHI tiền mặt NGOÀI bán hàng trong một ca — bảng {@code cash_movements}. Append-only
 * (giống sổ cái điểm / audit log): mỗi dòng là một khoản tiền vào ({@code IN}) hoặc ra ({@code OUT}) két.
 * Phục vụ đối soát quỹ cuối ca chính xác (giải thích chênh lệch do chi NCC, đổi tiền lẻ, rút bớt...).
 */
@Entity
@Table(name = "cash_movements")
@Getter
@Setter
@NoArgsConstructor
public class CashMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ca phát sinh khoản thu/chi. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private WorkShift shift;

    /** Chi nhánh (đa chuỗi) — = chi nhánh của ca. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CashMovementType type;

    /** Số tiền (dương) thu vào hoặc chi ra. */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    /** Lý do/diễn giải (bắt buộc) — vd "Trả tiền nước ngọt cho NCC", "Đổi tiền lẻ". */
    @Column(nullable = false, length = 255)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
