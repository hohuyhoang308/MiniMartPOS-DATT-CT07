package com.pos.repository;

import com.pos.entity.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {

    long countByCodeStartingWith(String prefix);

    /** Phiếu nhập của MỘT cửa hàng (đa cửa hàng), mới nhất trước. */
    List<GoodsReceipt> findByStoreIdOrderByCreatedAtDesc(Long storeId);
}
