package com.pos.entity;

import com.pos.entity.enums.ShiftStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Ca làm việc (FR4.1) — bảng {@code work_shifts}.
 *  Doanh thu ca KHÔNG lưu — suy ra qua view v_shift_summary. */
@Entity
@Table(name = "work_shifts")
@Getter
@Setter
@NoArgsConstructor
public class WorkShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Thu ngân của ca. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "opening_cash", nullable = false, precision = 12, scale = 2)
    private BigDecimal openingCash = BigDecimal.ZERO;

    /** Tiền đối soát cuối ca (NULL khi đang mở). */
    @Column(name = "closing_cash", precision = 12, scale = 2)
    private BigDecimal closingCash;

    @CreationTimestamp
    @Column(name = "opened_at", nullable = false, updatable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShiftStatus status = ShiftStatus.OPEN;
}
