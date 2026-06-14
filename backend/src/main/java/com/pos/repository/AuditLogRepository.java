package com.pos.repository;

import com.pos.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /** 200 vết kiểm toán mới nhất — màn xem nhật ký (ADMIN toàn chuỗi). */
    List<AuditLog> findTop200ByOrderByCreatedAtDesc();

    /** 200 vết mới nhất của MỘT cửa hàng — màn xem nhật ký của MANAGER. */
    List<AuditLog> findTop200ByStoreIdOrderByCreatedAtDesc(Long storeId);
}
