package com.pos.repository;

import com.pos.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    /**
     * Khóa dòng user (PESSIMISTIC_WRITE) — dùng làm mutex tuần tự hóa thao tác MỞ CA:
     * hai request mở ca đồng thời của cùng một người phải xếp hàng, request sau thấy
     * ca OPEN vừa tạo và bị từ chối (chặn race condition check-then-insert).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
}
