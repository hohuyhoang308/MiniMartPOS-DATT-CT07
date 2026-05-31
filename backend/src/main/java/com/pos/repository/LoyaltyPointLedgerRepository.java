package com.pos.repository;

import com.pos.entity.LoyaltyPointLedger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoyaltyPointLedgerRepository extends JpaRepository<LoyaltyPointLedger, Long> {

    /** Lịch sử điểm của 1 khách (mới nhất trước) — màn chi tiết khách hàng. */
    List<LoyaltyPointLedger> findTop100ByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
