package com.pos.repository;

import com.pos.entity.User;
import com.pos.entity.enums.Role;
import com.pos.entity.enums.UserStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    /** Tài khoản thuộc một cửa hàng — màn quản lý nhân viên của MANAGER (chỉ cửa hàng của mình). */
    List<User> findByStore_IdOrderByIdAsc(Long storeId);

    /** Tài khoản theo cửa hàng + vai trò — MANAGER chỉ quản lý STAFF của cửa hàng mình (khớp phạm vi GHI). */
    List<User> findByStore_IdAndRoleOrderByIdAsc(Long storeId, Role role);

    /** Đếm tài khoản theo vai trò + trạng thái — chặn hạ quyền/khóa QUẢN TRỊ VIÊN cuối cùng. */
    long countByRoleAndStatus(Role role, UserStatus status);

    /**
     * Khóa dòng user (PESSIMISTIC_WRITE) — dùng làm mutex tuần tự hóa thao tác MỞ CA:
     * hai request mở ca đồng thời của cùng một người phải xếp hàng, request sau thấy
     * ca OPEN vừa tạo và bị từ chối (chặn race condition check-then-insert).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
}
