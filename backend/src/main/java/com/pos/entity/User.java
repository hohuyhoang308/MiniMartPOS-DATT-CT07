package com.pos.entity;

import com.pos.entity.enums.Role;
import com.pos.entity.enums.UserStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Người dùng & phân quyền (FR1) — bảng {@code users}. */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** Mật khẩu băm BCrypt (~60 ký tự). */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /**
     * Chi nhánh người dùng trực thuộc (đa chuỗi). NULL = không gắn chi nhánh nào —
     * chỉ dùng cho {@link Role#CHAIN_ADMIN} (quản trị toàn chuỗi). ADMIN/MANAGER/CASHIER
     * BẮT BUỘC gắn một chi nhánh: mọi truy vấn của họ tự lọc theo chi nhánh này.
     *
     * <p>EAGER vì {@code User} là principal bảo mật: được nạp ở mỗi request (JWT filter) và
     * đọc {@code storeId/storeName} NGOÀI transaction (lúc đăng nhập, trong StoreContext) —
     * lazy sẽ gây LazyInitializationException.</p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "store_id")
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
