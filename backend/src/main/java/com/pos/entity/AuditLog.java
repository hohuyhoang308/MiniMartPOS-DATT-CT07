package com.pos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Nhật ký kiểm toán (audit log) — vết "ai làm gì, khi nào" cho hành động nhạy cảm. Append-only. */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_username", length = 50)
    private String actorUsername;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(name = "target_type", length = 40)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(length = 500)
    private String detail;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
