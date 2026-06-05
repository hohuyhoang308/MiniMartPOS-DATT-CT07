package com.pos.repository;

import com.pos.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    Optional<Promotion> findByCode(String code);

    boolean existsByCode(String code);

    /**
     * Tăng lượt dùng KM một cách NGUYÊN TỬ, chỉ khi chưa chạm usage_limit.
     * Trả về 1 nếu tăng được, 0 nếu đã hết lượt (hoặc id không tồn tại) → người gọi rollback giao dịch.
     * Tránh "lost update" khi nhiều đơn dùng cùng mã đồng thời.
     */
    @Modifying
    @Query("""
            UPDATE Promotion p SET p.usedCount = p.usedCount + 1
            WHERE p.id = :id AND (p.usageLimit IS NULL OR p.usedCount < p.usageLimit)
            """)
    int tryConsume(@Param("id") Long id);

    /** Giảm lượt dùng KM một cách NGUYÊN TỬ (không để âm) — khi hủy/đảo hóa đơn. */
    @Modifying
    @Query("UPDATE Promotion p SET p.usedCount = p.usedCount - 1 WHERE p.id = :id AND p.usedCount > 0")
    int releaseOne(@Param("id") Long id);
}
