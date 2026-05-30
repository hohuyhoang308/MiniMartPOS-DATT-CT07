package com.pos.repository;

import com.pos.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /** 200 vết kiểm toán mới nhất — màn xem nhật ký (admin). */
    List<AuditLog> findTop200ByOrderByCreatedAtDesc();
}
