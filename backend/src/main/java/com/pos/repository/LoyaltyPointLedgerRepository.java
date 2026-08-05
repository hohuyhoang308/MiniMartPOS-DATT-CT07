package com.pos.repository;

import com.pos.entity.LoyaltyPointLedger;
import com.pos.repository.projection.LoyaltyMismatchRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LoyaltyPointLedgerRepository extends JpaRepository<LoyaltyPointLedger, Long> {

    /** Số dòng sổ cái của một khách — để seed dòng MỞ khi khách có điểm ban đầu mà chưa có sổ cái. */
    long countByCustomerId(Long customerId);

    /**
     * Đối soát: liệt kê khách có số dư điểm (customers.loyalty_points) LỆCH với tổng sổ cái (Σ delta).
     * Sổ cái là nguồn truy vết append-only — lệch nghĩa là có chỗ cập nhật số dư mà quên ghi sổ (hoặc ngược lại).
     */
    @Query(value = """
            SELECT c.id AS customerId, c.full_name AS customerName,
                   c.loyalty_points AS stored, COALESCE(SUM(l.delta), 0) AS ledger
            FROM customers c
            LEFT JOIN loyalty_point_ledger l ON l.customer_id = c.id
            GROUP BY c.id, c.full_name, c.loyalty_points
            HAVING c.loyalty_points <> COALESCE(SUM(l.delta), 0)
            """, nativeQuery = true)
    List<LoyaltyMismatchRow> findPointMismatches();
}
