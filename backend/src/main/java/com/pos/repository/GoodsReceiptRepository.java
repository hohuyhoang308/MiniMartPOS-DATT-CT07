package com.pos.repository;

import com.pos.entity.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {

    long countByCodeStartingWith(String prefix);
}
